package com.example.uleammed.tendencias

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TendenciasRepository {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ─────────────────────────────────────────────────────────────────────
    //  Guardar snapshot — usa timestamp como ID del documento
    // ─────────────────────────────────────────────────────────────────────
    suspend fun saveSnapshot(snapshot: HealthSnapshot): Result<Unit> {
        return runCatching {
            val uid = auth.currentUser?.uid ?: error("Usuario no autenticado")

            val computedGlobal = listOf(
                snapshot.estresIndex, snapshot.ergonomiaIndex,
                snapshot.cargaTrabajoIndex, snapshot.calidadSuenoIndex,
                snapshot.actividadFisicaIndex, snapshot.sintomasMuscularesIndex,
                snapshot.sintomasVisualesIndex, snapshot.saludGeneralIndex
            ).average().toFloat() * 10f

            val ts = System.currentTimeMillis()

            firestore
                .collection(USERS).document(uid)
                .collection(SCORE_HISTORY)
                .document(ts.toString())
                .set(mapOf(
                    "timestamp"               to ts,
                    "estresIndex"             to snapshot.estresIndex,
                    "ergonomiaIndex"          to snapshot.ergonomiaIndex,
                    "cargaTrabajoIndex"       to snapshot.cargaTrabajoIndex,
                    "calidadSuenoIndex"       to snapshot.calidadSuenoIndex,
                    "actividadFisicaIndex"    to snapshot.actividadFisicaIndex,
                    "sintomasMuscularesIndex" to snapshot.sintomasMuscularesIndex,
                    "sintomasVisualesIndex"   to snapshot.sintomasVisualesIndex,
                    "saludGeneralIndex"       to snapshot.saludGeneralIndex,
                    "nivelRiesgo"             to snapshot.nivelRiesgo,
                    "confianza"               to snapshot.confianza,
                    "globalScore"             to computedGlobal
                ))
                .await()

            Unit
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Leer score_history sin índices compuestos:
    //  - get() sin orderBy → no requiere índice
    //  - ordenar y filtrar en memoria
    // ─────────────────────────────────────────────────────────────────────
    suspend fun getLast4PeriodSnapshots(): Result<Pair<CollectionPeriod, List<PeriodHealthSnapshot>>> {
        return runCatching {
            val uid = auth.currentUser?.uid ?: error("Usuario no autenticado")

            val periodDays = getUserPeriodDays(uid)
            val period     = CollectionPeriod.fromDays(periodDays)

            // Sin orderBy ni whereGreaterThan — trae todo y filtra en memoria
            val docs = firestore
                .collection(USERS).document(uid)
                .collection(SCORE_HISTORY)
                .get().await()
                .documents

            if (docs.isEmpty()) return@runCatching Pair(period, emptyList())

            // Parsear y ordenar por timestamp en memoria
            val snapshots = docs.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                val ts = (d["timestamp"] as? Long)
                    ?: (d["timestamp"] as? Number)?.toLong()
                    ?: doc.id.toLongOrNull()
                    ?: return@mapNotNull null

                HealthSnapshot(
                    timestamp                 = ts,
                    estresIndex               = d.float("estresIndex"),
                    ergonomiaIndex            = d.float("ergonomiaIndex"),
                    cargaTrabajoIndex         = d.float("cargaTrabajoIndex"),
                    calidadSuenoIndex         = d.float("calidadSuenoIndex"),
                    actividadFisicaIndex      = d.float("actividadFisicaIndex"),
                    sintomasMuscularesIndex   = d.float("sintomasMuscularesIndex"),
                    sintomasVisualesIndex     = d.float("sintomasVisualesIndex"),
                    saludGeneralIndex         = d.float("saludGeneralIndex"),
                    nivelRiesgo               = (d["nivelRiesgo"] as? String) ?: "BAJO",
                    confianza                 = d.float("confianza"),
                    storedGlobalScore         = (d["globalScore"] as? Number)?.toFloat()
                )
            }.sortedBy { it.timestamp }  // ordenar en memoria — sin índice necesario

            // Filtrar ventana de 4 períodos desde el más reciente
            val windowMs = period.days * 4L * 24 * 60 * 60 * 1000
            val cutoff   = snapshots.last().timestamp - windowMs
            val inWindow = snapshots.filter { it.timestamp >= cutoff }

            val toGroup = if (inWindow.size >= 2) inWindow else snapshots

            Pair(period, groupByPeriod(toGroup, period))
        }
    }

    private fun groupByPeriod(
        snapshots: List<HealthSnapshot>,
        period: CollectionPeriod
    ): List<PeriodHealthSnapshot> {
        if (snapshots.isEmpty()) return emptyList()

        val periodMs  = period.days * 24L * 60 * 60 * 1000
        val firstTs   = snapshots.first().timestamp
        val lastTs    = snapshots.last().timestamp
        val numBlocks = (((lastTs - firstTs) / periodMs) + 1).toInt().coerceIn(1, 4)

        val byBlock = snapshots.groupBy { snap ->
            ((snap.timestamp - firstTs) / periodMs).toInt().coerceIn(0, numBlocks - 1)
        }

        return byBlock.entries.sortedBy { it.key }.mapIndexed { index, (_, blockSnaps) ->
            PeriodHealthSnapshot(
                periodLabel          = "${period.label} ${index + 1}",
                periodStart          = blockSnaps.first().timestamp,
                estresScore          = blockSnaps.map { it.estresIndex          * 10f }.avg(),
                ergonomiaScore       = blockSnaps.map { it.ergonomiaIndex       * 10f }.avg(),
                cargaTrabajoScore    = blockSnaps.map { it.cargaTrabajoIndex    * 10f }.avg(),
                calidadSuenoScore    = blockSnaps.map { it.calidadSuenoIndex    * 10f }.avg(),
                actividadFisicaScore = blockSnaps.map { it.actividadFisicaIndex * 10f }.avg(),
                musculoScore         = blockSnaps.map { it.sintomasMuscularesIndex * 10f }.avg(),
                visualScore          = blockSnaps.map { it.sintomasVisualesIndex   * 10f }.avg(),
                saludGeneralScore    = blockSnaps.map { it.saludGeneralIndex    * 10f }.avg(),
                globalScore          = blockSnaps.map { it.effectiveGlobalScore }.avg()
            )
        }
    }

    private suspend fun getUserPeriodDays(uid: String): Int {
        return try {
            val doc = firestore
                .collection(USERS).document(uid)
                .collection(SETTINGS).document(NOTIFICATIONS)
                .get().await()
            doc.getLong(FIELD_INTERVAL_DAYS)?.toInt() ?: DEFAULT_PERIOD_DAYS
        } catch (e: Exception) {
            DEFAULT_PERIOD_DAYS
        }
    }

    private fun Map<String, Any>.float(key: String): Float =
        (this[key] as? Number)?.toFloat() ?: 0f

    private fun List<Float>.avg(): Float =
        if (isEmpty()) 0f else average().toFloat()

    companion object {
        private const val USERS               = "users"
        private const val SCORE_HISTORY       = "score_history"
        private const val SETTINGS            = "settings"
        private const val NOTIFICATIONS       = "notifications"
        private const val FIELD_INTERVAL_DAYS = "intervalDays"
        private const val DEFAULT_PERIOD_DAYS = 7
    }
}