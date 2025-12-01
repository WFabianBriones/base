package com.example.uleammed.scoring

import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.questionnaires.*

/**
 * Sistema de scoring basado en estándares internacionales
 */

// ==================== NIVELES DE RIESGO ====================
enum class RiskLevel(val value: Int, val displayName: String, val color: Long) {
    BAJO(1, "Bajo", 0xFF4CAF50),
    MODERADO(2, "Moderado", 0xFFFFC107),
    ALTO(3, "Alto", 0xFFFF9800),
    MUY_ALTO(4, "Muy Alto", 0xFFF44336)
}

// ==================== RESULTADO GENERAL ====================
data class HealthScore(
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    // ✅ NUEVO: Salud General (cuestionario inicial)
    val saludGeneralScore: Int = 0,
    val saludGeneralRisk: RiskLevel = RiskLevel.BAJO,

    // Scores individuales (0-100)
    val ergonomiaScore: Int = 0,
    val sintomasMuscularesScore: Int = 0,
    val sintomasVisualesScore: Int = 0,
    val cargaTrabajoScore: Int = 0,
    val estresSaludMentalScore: Int = 0,
    val habitosSuenoScore: Int = 0,
    val actividadFisicaScore: Int = 0,
    val balanceVidaTrabajoScore: Int = 0,

    // Niveles de riesgo
    val ergonomiaRisk: RiskLevel = RiskLevel.BAJO,
    val sintomasMuscularesRisk: RiskLevel = RiskLevel.BAJO,
    val sintomasVisualesRisk: RiskLevel = RiskLevel.BAJO,
    val cargaTrabajoRisk: RiskLevel = RiskLevel.BAJO,
    val estresSaludMentalRisk: RiskLevel = RiskLevel.BAJO,
    val habitosSuenoRisk: RiskLevel = RiskLevel.BAJO,
    val actividadFisicaRisk: RiskLevel = RiskLevel.BAJO,
    val balanceVidaTrabajoRisk: RiskLevel = RiskLevel.BAJO,

    // Score global
    val overallScore: Int = 0,
    val overallRisk: RiskLevel = RiskLevel.BAJO,

    // Áreas de mejora
    val topConcerns: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

// ==================== CALCULADOR DE SCORES ====================
object ScoreCalculator {

    /**
     * 0. SALUD GENERAL - Cuestionario Inicial (Score 0-100, mayor es peor)
     */
    fun calculateHealthQuestionnaireScore(q: HealthQuestionnaire): Pair<Int, RiskLevel> {
        var score = 0

        // IMC (0-15 puntos)
        when {
            q.bmiCategory.contains("Obesidad") -> score += 15
            q.bmiCategory.contains("Sobrepeso") -> score += 10
            q.bmiCategory.contains("Bajo peso") -> score += 8
        }

        // Hábitos (0-20 puntos)
        when {
            q.smokingStatus.contains("más de 10") -> score += 15
            q.smokingStatus.contains("menos de 10") -> score += 10
            q.smokingStatus.contains("ocasionalmente") -> score += 5
        }

        when {
            q.alcoholConsumption.contains("Frecuente") -> score += 5
        }

        // Condiciones médicas (0-25 puntos)
        val seriousConditions = listOf("Diabetes", "Hipertensión", "Problemas cardíacos",
            "Ansiedad/Depresión", "Hernia discal")
        val userConditions = q.preexistingConditions.filter { it in seriousConditions }
        score += userConditions.size * 5

        // Medicamentos (0-10 puntos)
        if (q.medications.size > 2 && !q.medications.contains("Ninguno")) {
            score += 10
        }

        // Cirugías (0-5 puntos)
        if (q.recentSurgeries) score += 5

        // Estado general (0-15 puntos)
        when {
            q.energyLevel.contains("Muy bajo") -> score += 10
            q.energyLevel.contains("Bajo") -> score += 7
        }

        when {
            q.generalHealthStatus == "Malo" -> score += 10
            q.generalHealthStatus == "Regular" -> score += 5
        }

        // COVID largo (0-10 puntos)
        if (q.hadCovid.contains("secuelas persistentes")) {
            score += 10
        }

        // Indicadores (0-10 puntos)
        if (q.bloodPressure.contains("Hipertensión")) score += 5
        if (q.cholesterolLevel.contains("Alto")) score += 3
        if (q.bloodGlucose.contains("Diabetes")) score += 5

        score = score.coerceIn(0, 100)

        val risk = when {
            score < 20 -> RiskLevel.BAJO
            score < 40 -> RiskLevel.MODERADO
            score < 60 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 1. ERGONOMÍA (Score 0-100, mayor es mejor)
     */
    fun calculateErgonomiaScore(q: ErgonomiaQuestionnaire): Pair<Int, RiskLevel> {
        var score = 100

        // Mobiliario (30 puntos)
        if (q.tipoSilla.contains("básica") || q.tipoSilla.contains("inadecuada")) score -= 8
        if (q.tipoSilla.contains("sin ajustes")) score -= 5
        if (q.soporteLumbar == "No tiene") score -= 7
        if (q.alturaEscritorio.contains("alto") || q.alturaEscritorio.contains("bajo")) score -= 5
        if (q.espacioEscritorio.contains("reducido")) score -= 5

        // Monitor (25 puntos)
        if (q.tipoMonitor.contains("Solo laptop (sin soporte)")) score -= 8
        if (q.alturaMonitor.contains("encima") || q.alturaMonitor.contains("más de 15cm")) score -= 7
        if (q.distanciaMonitor.contains("cerca") || q.distanciaMonitor.contains("lejos")) score -= 5
        if (q.posicionTeclado.contains("encima") || q.posicionTeclado.contains("debajo")) score -= 5

        // Iluminación (20 puntos)
        if (q.iluminacionPrincipal.contains("Insuficiente")) score -= 10
        if (q.reflejosPantalla == "Constantemente" || q.reflejosPantalla == "Frecuentemente") score -= 7
        if (q.lamparaEscritorio == "No") score -= 3

        // Ambiente (15 puntos)
        if (q.temperatura.contains("Frío") || q.temperatura.contains("Calor")) score -= 5
        if (q.nivelRuido == "Muy ruidoso" || q.nivelRuido == "Ruidoso") score -= 5
        if (q.ventilacion.contains("Mala") || q.ventilacion.contains("Regular")) score -= 5

        // Pausas (10 puntos)
        if (q.pausasActivas.contains("Nunca") || q.pausasActivas.contains("rara vez")) score -= 5
        if (q.tiempoSentadoContinuo.contains("Más de 3 horas")) score -= 5

        val risk = when {
            score >= 80 -> RiskLevel.BAJO
            score >= 60 -> RiskLevel.MODERADO
            score >= 40 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score.coerceIn(0, 100), risk)
    }

    /**
     * 2. SÍNTOMAS MUSCULARES (Score 0-100, mayor es peor)
     */
    fun calculateSintomasMuscularesScore(q: SintomasMuscularesQuestionnaire): Pair<Int, RiskLevel> {
        var totalSymptoms = 0
        var maxIntensity = 0

        // Cuello (peso: alto)
        val cuelloScore = (q.dolorCuelloFrecuencia + q.dolorCuelloIntensidad +
                q.rigidezCuelloFrecuencia + q.rigidezCuelloIntensidad) / 4.0
        totalSymptoms += (cuelloScore * 2).toInt()
        maxIntensity = maxOf(maxIntensity, q.dolorCuelloIntensidad, q.rigidezCuelloIntensidad)

        // Hombros (peso: alto)
        val hombrosScore = (q.dolorHombrosFrecuencia + q.dolorHombrosIntensidad) / 2.0
        totalSymptoms += (hombrosScore * 2).toInt()
        maxIntensity = maxOf(maxIntensity, q.dolorHombrosIntensidad)

        // Espalda (peso: muy alto)
        val espaldaScore = (q.dolorEspaldaAltaFrecuencia + q.dolorEspaldaAltaIntensidad +
                q.dolorEspaldaBajaFrecuencia + q.dolorEspaldaBajaIntensidad +
                q.rigidezEspaldaMañanaFrecuencia + q.rigidezEspaldaMañanaIntensidad) / 6.0
        totalSymptoms += (espaldaScore * 3).toInt()
        maxIntensity = maxOf(maxIntensity, q.dolorEspaldaAltaIntensidad, q.dolorEspaldaBajaIntensidad)

        // Manos/muñecas (peso: alto)
        val manosScore = (q.dolorMunecasFrecuencia + q.dolorMunecasIntensidad +
                q.dolorManosFrecuencia + q.dolorManosIntensidad +
                q.hormigueoManosFrecuencia + q.hormigueoManosIntensidad) / 6.0
        totalSymptoms += (manosScore * 2).toInt()

        if (q.hormigueoPorNoche.contains("despierta")) totalSymptoms += 5

        // Dolor de cabeza
        val cabezaScore = (q.dolorCabezaFrecuencia + q.dolorCabezaIntensidad) / 2.0
        totalSymptoms += cabezaScore.toInt()

        // Impacto funcional
        if (q.dolorImpidenActividades.contains("frecuentemente")) totalSymptoms += 10
        else if (q.dolorImpidenActividades.contains("ocasionalmente")) totalSymptoms += 5

        val score = ((totalSymptoms / 50.0) * 100).toInt().coerceIn(0, 100)

        val risk = when {
            score < 20 || maxIntensity <= 1 -> RiskLevel.BAJO
            score < 40 || maxIntensity <= 2 -> RiskLevel.MODERADO
            score < 60 || maxIntensity <= 3 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 3. SÍNTOMAS VISUALES (Score 0-100, mayor es peor)
     */
    fun calculateSintomasVisualesScore(q: SintomasVisualesQuestionnaire): Pair<Int, RiskLevel> {
        var totalSymptoms = 0

        totalSymptoms += q.ojosSecosFrecuencia * 2 + q.ojosSecosIntensidad
        totalSymptoms += q.ardorOjosFrecuencia * 2 + q.ardorOjosIntensidad
        totalSymptoms += q.ojosRojosFrecuencia * 2
        totalSymptoms += q.lagrimeoFrecuencia

        totalSymptoms += q.visionBorrosaFrecuencia * 3
        totalSymptoms += q.dificultadEnfocarFrecuencia * 3
        totalSymptoms += q.sensibilidadLuzFrecuencia * 2
        totalSymptoms += q.visionDobleFrecuencia * 4

        if (q.ojosCansadosFinDia == "Siempre") totalSymptoms += 8
        else if (q.ojosCansadosFinDia == "Frecuentemente") totalSymptoms += 5

        if (q.esfuerzoVerNitidamente.contains("constantemente")) totalSymptoms += 8
        else if (q.esfuerzoVerNitidamente.contains("final del día")) totalSymptoms += 5

        if (q.aplicaRegla202020.contains("Nunca") || q.aplicaRegla202020.contains("no sé")) totalSymptoms += 5
        if (q.ultimoExamenVisual.contains("Nunca") || q.ultimoExamenVisual.contains("más de 2 años")) totalSymptoms += 5

        val score = ((totalSymptoms / 80.0) * 100).toInt().coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 50 -> RiskLevel.MODERADO
            score < 70 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 4. CARGA DE TRABAJO (Score 0-100, mayor es peor)
     */
    fun calculateCargaTrabajoScore(q: CargaTrabajoQuestionnaire): Pair<Int, RiskLevel> {
        var demandScore = 0
        var controlScore = 0
        var supportScore = 0

        // DEMANDA
        when (q.cargaTrabajoActual) {
            "Excesiva (no puedo con todo)" -> demandScore += 10
            "Alta (requiere esfuerzo extra)" -> demandScore += 7
            "Adecuada (equilibrada)" -> demandScore += 3
        }

        when (q.presionTiempoPlazos) {
            "Muy alta/Constante" -> demandScore += 10
            "Alta" -> demandScore += 7
            "Moderada" -> demandScore += 4
        }

        when (q.ritmoTrabajo) {
            "Frenético/Agobiante" -> demandScore += 10
            "Acelerado frecuentemente" -> demandScore += 6
        }

        if (q.llevaTrabajoCasa.contains("Siempre") || q.llevaTrabajoCasa.contains("Muy frecuentemente")) {
            demandScore += 5
        }
        if (q.trabajaFinesSemana.contains("Todos") || q.trabajaFinesSemana.contains("3 fines")) {
            demandScore += 5
        }

        // CONTROL
        when (q.puedeDecidirComoTrabajar) {
            "No, todo está muy controlado" -> controlScore += 10
            "Poco" -> controlScore += 7
            "Parcialmente" -> controlScore += 4
        }

        when (q.puedePlanificarPausas) {
            "No puedo" -> controlScore += 10
            "Con dificultad" -> controlScore += 6
        }

        when (q.participaDecisiones) {
            "Nunca" -> controlScore += 10
            "Rara vez" -> controlScore += 6
        }

        // APOYO
        when (q.apoyoSuperior) {
            "Muy malo/Ninguno" -> supportScore += 8
            "Malo" -> supportScore += 6
            "Regular" -> supportScore += 3
        }

        when (q.relacionCompaneros) {
            "Mala (conflictiva)" -> supportScore += 7
            "Regular" -> supportScore += 3
        }

        if (q.acosoLaboral.contains("Constantemente") || q.acosoLaboral.contains("Frecuentemente")) {
            supportScore += 15
        }

        var satisfactionPenalty = 0
        if (q.satisfaccionGeneral.contains("Muy insatisfecho")) satisfactionPenalty += 5
        if (q.trabajoValorado.contains("Nunca") || q.trabajoValorado.contains("Rara vez")) {
            satisfactionPenalty += 5
        }

        val totalScore = demandScore + controlScore + supportScore + satisfactionPenalty
        val score = ((totalScore / 100.0) * 100).toInt().coerceIn(0, 100)

        val risk = when {
            score < 30 -> RiskLevel.BAJO
            score < 50 -> RiskLevel.MODERADO
            score < 70 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 5. ESTRÉS Y SALUD MENTAL (Score 0-100, mayor es peor)
     */
    fun calculateEstresSaludMentalScore(q: EstresSaludMentalQuestionnaire): Pair<Int, RiskLevel> {
        var score = 0

        score += q.nivelEstresGeneral * 1.5.toInt()
        if (q.estresAumento6Meses.contains("significativamente")) score += 5

        val stressSymptoms = listOf(
            q.fatigaAgotamiento, q.dificultadConcentracion, q.problemasMemoria,
            q.irritabilidad, q.ansiedadTrabajo, q.preocupacionesConstantes,
            q.sensacionAbrumado, q.dificultadDesconectar
        )
        val avgStressSymptoms = stressSymptoms.average()
        score += (avgStressSymptoms * 8).toInt()

        val burnoutSymptoms = listOf(
            q.perdidaMotivacion, q.sensacionInproductividad,
            q.actitudNegativa, q.sentimientoIneficacia
        )
        val avgBurnout = burnoutSymptoms.average()
        score += (avgBurnout * 8).toInt()

        if (q.agotamientoEmocional.contains("Constantemente")) score += 10
        else if (q.agotamientoEmocional.contains("Frecuentemente")) score += 7

        if (q.despersonalizacion.contains("Constantemente")) score += 10
        else if (q.despersonalizacion.contains("Frecuentemente")) score += 7

        if (q.estresAfectaVidaPersonal.contains("Severamente")) score += 10
        else if (q.estresAfectaVidaPersonal.contains("Significativamente")) score += 7

        score = score.coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 45 -> RiskLevel.MODERADO
            score < 65 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 6. HÁBITOS DE SUEÑO (Score 0-100, mayor es peor)
     */
    fun calculateHabitosSuenoScore(q: HabitosSuenoQuestionnaire): Pair<Int, RiskLevel> {
        var score = 0

        when (q.horasSuenoSemana) {
            "Menos de 5 horas" -> score += 15
            "5-6 horas" -> score += 10
            "Más de 8 horas" -> score += 5
        }

        if (q.horasSuenoFinSemana.contains("3-4 horas más")) score += 10

        when (q.calidadSueno) {
            "Muy mala (nunca descansado)" -> score += 20
            "Mala (rara vez descansado)" -> score += 15
            "Regular (a veces descansado)" -> score += 10
        }

        val sleepProblems = listOf(
            q.dificultadConciliarFrecuencia,
            q.despertaresNocturnosFrecuencia,
            q.despertarTempranoFrecuencia
        )
        val avgProblems = sleepProblems.average()
        score += (avgProblems * 6).toInt()

        if (q.usaDispositivosAntesDormir.contains("hasta el momento de dormir")) score += 10

        if (q.piensaProblemasTrabajoAntesDormir.contains("Siempre") ||
            q.piensaProblemasTrabajoAntesDormir.contains("Muy frecuentemente")) {
            score += 10
        }

        if (q.revisaCorreosFueraHorario.contains("constantemente") ||
            q.revisaCorreosFueraHorario.contains("varias veces")) {
            score += 5
        }

        score = score.coerceIn(0, 100)

        val risk = when {
            score < 20 -> RiskLevel.BAJO
            score < 40 -> RiskLevel.MODERADO
            score < 60 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 7. ACTIVIDAD FÍSICA (Score 0-100, mayor es peor)
     */
    fun calculateActividadFisicaScore(q: ActividadFisicaQuestionnaire): Pair<Int, RiskLevel> {
        var score = 0

        when (q.frecuenciaEjercicio) {
            "Ninguna (sedentario)" -> score += 20
            "1 vez por semana" -> score += 15
            "2-3 veces por semana" -> score += 5
        }

        when (q.duracionEjercicio) {
            "No hago ejercicio" -> score += 10
            "Menos de 20 minutos" -> score += 7
        }

        when (q.realizaEstiramientos) {
            "Nunca" -> score += 10
            "Rara vez" -> score += 7
            "Ocasionalmente" -> score += 4
        }

        if (q.saltaDesayuno.contains("Siempre") || q.saltaDesayuno.contains("Frecuentemente")) {
            score += 10
        }

        if (q.comeEnEscritorio.contains("Siempre") || q.comeEnEscritorio.contains("Frecuentemente")) {
            score += 5
        }

        when (q.consumoAguaDiario) {
            "Menos de 1 litro" -> score += 10
        }

        when (q.consumoCafeTe) {
            "Más de 5 tazas al día" -> score += 15
            "4-5 tazas al día" -> score += 10
        }

        when (q.consumeBebidasEnergizantes) {
            "Diariamente" -> score += 20
            "Frecuentemente (3+ por semana)" -> score += 15
            "Regularmente (1-2 por semana)" -> score += 10
        }

        score = score.coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 45 -> RiskLevel.MODERADO
            score < 65 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * 8. BALANCE VIDA-TRABAJO (Score 0-100, mayor es peor)
     */
    fun calculateBalanceVidaTrabajoScore(q: BalanceVidaTrabajoQuestionnaire): Pair<Int, RiskLevel> {
        var score = 0

        when (q.equilibrioTrabajoVida) {
            "El trabajo domina completamente mi vida" -> score += 20
            "Más trabajo que vida personal" -> score += 15
            "Parcialmente equilibrado" -> score += 8
        }

        when (q.tiempoLibreCalidad) {
            "Menos de 5 horas" -> score += 15
            "5-10 horas" -> score += 10
            "10-15 horas" -> score += 5
        }

        when (q.actividadesRecreativas) {
            "Nunca, no tengo tiempo" -> score += 15
            "Rara vez" -> score += 10
            "Ocasionalmente (1-2 veces al mes)" -> score += 6
        }

        when (q.trabajoAfectaRelaciones) {
            "Severamente" -> score += 20
            "Bastante" -> score += 15
            "Moderadamente" -> score += 10
        }

        when (q.tiempoFamiliaAmigos) {
            "Casi ninguno" -> score += 10
            "Menos de 2 horas" -> score += 7
        }

        when (q.puedeDesconectarseDiasLibres) {
            "Nunca puedo desconectar" -> score += 10
            "Rara vez" -> score += 7
            "Con dificultad" -> score += 4
        }

        when (q.revisaCorreosVacaciones) {
            "Constantemente" -> score += 10
            "Frecuentemente" -> score += 7
        }

        when (q.ultimasVacaciones) {
            "Nunca/No recuerdo" -> score += 10
            "Hace más de 2 años" -> score += 7
            "Hace 1-2 años" -> score += 4
        }

        score = score.coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 45 -> RiskLevel.MODERADO
            score < 65 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(score, risk)
    }

    /**
     * SCORE GLOBAL PONDERADO - 9 ÁREAS
     */
    fun calculateOverallScore(scores: Map<String, Pair<Int, RiskLevel>>): Pair<Int, RiskLevel> {
        val weights = mapOf(
            "salud_general" to 0.10,
            "estres" to 0.18,
            "sintomas_musculares" to 0.16,
            "carga_trabajo" to 0.14,
            "sueno" to 0.11,
            "balance" to 0.11,
            "ergonomia" to 0.09,
            "sintomas_visuales" to 0.07,
            "actividad_fisica" to 0.04
        )

        var weightedSum = 0.0
        var totalWeight = 0.0
        var highestRisk = RiskLevel.BAJO

        scores.forEach { (key, pair) ->
            val weight = weights[key] ?: 0.0
            weightedSum += pair.first * weight
            totalWeight += weight

            if (pair.second.value > highestRisk.value) {
                highestRisk = pair.second
            }
        }

        val overallScore = if (totalWeight > 0) {
            (weightedSum / totalWeight).toInt()
        } else {
            0
        }

        val calculatedRisk = when {
            overallScore < 25 -> RiskLevel.BAJO
            overallScore < 45 -> RiskLevel.MODERADO
            overallScore < 65 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        val finalRisk = if (highestRisk.value > calculatedRisk.value) highestRisk else calculatedRisk

        return Pair(overallScore, finalRisk)
    }
}