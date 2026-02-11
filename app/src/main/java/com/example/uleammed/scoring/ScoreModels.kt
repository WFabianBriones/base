package com.example.uleammed.scoring

import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.questionnaires.*

/**
 * ✅ SISTEMA DE SCORING MEJORADO - PRIORIDAD ALTA IMPLEMENTADA
 *
 * Cambios principales:
 * 1. ✅ Máximos teóricos recalculados dinámicamente
 * 2. ✅ Detección de patrones críticos implementada
 * 3. ✅ Corrección de multiplicación en estrés
 * 4. ✅ Validación estricta de scores (nunca superan 100)
 */

// ==================== NIVELES DE RIESGO ====================
enum class RiskLevel(val value: Int, val displayName: String, val color: Long) {
    BAJO(1, "Bajo", 0xFF4CAF50),
    MODERADO(2, "Moderado", 0xFFFFC107),
    ALTO(3, "Alto", 0xFFFF9800),
    MUY_ALTO(4, "Muy Alto", 0xFFF44336)
}

// ==================== PATRONES CRÍTICOS ====================

/**
 * ✅ NUEVO: Niveles de criticidad para alertas tempranas
 */
enum class CriticalLevel {
    ALERTA_TEMPRANA,        // Situación a monitorear
    ATENCION_REQUERIDA,     // Requiere intervención preventiva
    INTERVENCION_URGENTE    // Requiere atención médica/profesional inmediata
}

/**
 * ✅ NUEVO: Patrón crítico detectado en cuestionarios
 */
data class CriticalPattern(
    val area: String,
    val severity: CriticalLevel,
    val description: String,
    val recommendation: String
)

// ==================== VALIDACIÓN ====================

/**
 * ✅ MEJORADO: Resultado de validación con más detalles
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList() // NUEVO: advertencias no críticas
)

// ==================== RESULTADO GENERAL ====================
data class HealthScore(
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val version: Int = 2, // ✅ ACTUALIZADO: Nueva versión con mejoras
    val lastUpdated: Map<String, Long> = mapOf(
        "salud_general" to 0L,
        "ergonomia" to 0L,
        "sintomas_musculares" to 0L,
        "sintomas_visuales" to 0L,
        "carga_trabajo" to 0L,
        "estres" to 0L,
        "sueno" to 0L,
        "actividad_fisica" to 0L,
        "balance" to 0L
    ),

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
    val recommendations: List<String> = emptyList(),

    // ✅ NUEVO: Patrones críticos detectados
    val criticalPatterns: List<CriticalPattern> = emptyList()
)

// ==================== CALCULADOR DE SCORES ====================
object ScoreCalculator {

    private const val TAG = "ScoreCalculator"

    // ==================== MÁXIMOS TEÓRICOS ====================

    /**
     * ✅ NUEVO: Máximos teóricos calculados dinámicamente
     */
    object MaxScores {
        // Salud General
        const val SALUD_GENERAL_MAX = 103 // IMC(15) + Hábitos(20) + Condiciones(25-ilimitado,limitado a 25) + Meds(10) + Cirugías(5) + Estado(15) + COVID(10) + Indicadores(13)

        // Ergonomía (invertida: 100 es mejor)
        const val ERGONOMIA_MAX = 100

        // Síntomas Musculares: calculado dinámicamente
        fun getSintomasMuscularesMax(): Double {
            // Cuello: 2 síntomas × ((frec + int) / 4) × peso 2 = (10/4)*2 = 5
            // Hombros: 1 síntoma × ((frec + int) / 2) × peso 2 = (10/2)*2 = 10
            // Espalda: 3 síntomas × ((frec + int) / 6) × peso 3 = (30/6)*3 = 15
            // Manos: 3 síntomas × ((frec + int) / 6) × peso 2 = (30/6)*2 = 10
            // Cabeza: 1 síntoma × ((frec + int) / 2) = (10/2) = 5
            // Bonificaciones: 10 + 5 = 15
            return 5.0 + 10.0 + 15.0 + 10.0 + 5.0 + 15.0 // = 60
        }

        // Síntomas Visuales
        fun getSintomasVisualesMax(): Double {
            // Ojos secos: (5*2 + 5) = 15
            // Ardor: (5*2 + 5) = 15
            // Ojos rojos: 5*2 = 10
            // Lagrimeo: 5 = 5
            // Visión borrosa: 5*3 = 15
            // Dificultad enfocar: 5*3 = 15
            // Sensibilidad luz: 5*2 = 10
            // Visión doble: 5*4 = 20
            // Cansancio fin día: 8
            // Esfuerzo ver: 8
            // No aplica regla: 5
            // Examen visual: 5
            return 131.0
        }

        // Carga de Trabajo
        const val CARGA_TRABAJO_MAX = 110 // Demanda(40) + Control(30) + Apoyo(30) + Satisfacción(10)

        // Estrés y Salud Mental
        fun getEstresMax(): Double {
            // Nivel general: 5 * 1.5 = 7.5
            // Aumento 6 meses: 5
            // 8 síntomas de estrés promedio × 8: 5*8 = 40
            // 4 síntomas de burnout promedio × 8: 5*8 = 40
            // Agotamiento emocional: 10
            // Despersonalización: 10
            // Afecta vida personal: 10
            return 122.5
        }

        // Hábitos de Sueño
        const val SUENO_MAX = 95 // Horas semana(15) + diferencia finde(10) + calidad(20) + problemas promedio*6(30) + dispositivos(10) + preocupaciones(10)

        // Actividad Física
        const val ACTIVIDAD_FISICA_MAX = 100

        // Balance Vida-Trabajo
        const val BALANCE_MAX = 100
    }

    // ==================== DETECCIÓN DE PATRONES CRÍTICOS ====================

    /**
     * ✅ NUEVO: Detecta patrones críticos en síntomas musculares
     */
    fun detectMusculoskeletalPatterns(q: SintomasMuscularesQuestionnaire): List<CriticalPattern> {
        val patterns = mutableListOf<CriticalPattern>()

        // PATRÓN 1: Síndrome del Túnel Carpiano
        if (q.hormigueoPorNoche.contains("despierta")) {
            patterns.add(CriticalPattern(
                area = "Manos/Muñecas",
                severity = CriticalLevel.ATENCION_REQUERIDA,
                description = "Hormigueo nocturno que interrumpe el sueño",
                recommendation = "Consultar con médico especialista. Posible síndrome del túnel carpiano."
            ))
        }

        // PATRÓN 2: Dolor lumbar severo persistente
        if (q.dolorEspaldaBajaFrecuencia >= 4 && q.dolorEspaldaBajaIntensidad >= 4) {
            patterns.add(CriticalPattern(
                area = "Espalda Baja",
                severity = CriticalLevel.INTERVENCION_URGENTE,
                description = "Dolor lumbar severo y frecuente",
                recommendation = "Consulta médica urgente. Evaluación de postura y ergonomía del puesto de trabajo."
            ))
        }

        // PATRÓN 3: Dolor cervical crónico
        if (q.dolorCuelloFrecuencia >= 4 && q.dolorCuelloIntensidad >= 3) {
            patterns.add(CriticalPattern(
                area = "Cuello",
                severity = CriticalLevel.ATENCION_REQUERIDA,
                description = "Dolor cervical frecuente y moderado/severo",
                recommendation = "Revisar altura de monitor y postura. Considerar evaluación médica."
            ))
        }

        // PATRÓN 4: Dolor generalizado (3+ áreas afectadas)
        val affectedAreas = listOf(
            q.dolorCuelloFrecuencia >= 3,
            q.dolorHombrosFrecuencia >= 3,
            q.dolorEspaldaBajaFrecuencia >= 3,
            q.dolorManosFrecuencia >= 3
        ).count { it }

        if (affectedAreas >= 3) {
            patterns.add(CriticalPattern(
                area = "Múltiples Zonas",
                severity = CriticalLevel.ATENCION_REQUERIDA,
                description = "Dolor en 3 o más áreas corporales",
                recommendation = "Evaluación ergonómica integral del puesto de trabajo. Considerar valoración médica."
            ))
        }

        // PATRÓN 5: Impacto funcional severo
        if (q.dolorImpidenActividades.contains("frecuentemente")) {
            patterns.add(CriticalPattern(
                area = "Capacidad Funcional",
                severity = CriticalLevel.INTERVENCION_URGENTE,
                description = "Dolor que interfiere frecuentemente con actividades diarias",
                recommendation = "Consulta médica inmediata. Evaluación de incapacidad temporal si es necesario."
            ))
        }

        // PATRÓN 6: Cefalea tensional severa
        if (q.dolorCabezaFrecuencia >= 4 && q.dolorCabezaIntensidad >= 3) {
            patterns.add(CriticalPattern(
                area = "Cabeza",
                severity = CriticalLevel.ATENCION_REQUERIDA,
                description = "Cefaleas frecuentes y moderadas/severas",
                recommendation = "Consulta médica. Revisar iluminación, postura y pausas activas."
            ))
        }

        return patterns
    }

    /**
     * ✅ NUEVO: Detecta patrones críticos en estrés y salud mental
     */
    fun detectStressPatterns(q: EstresSaludMentalQuestionnaire): List<CriticalPattern> {
        val patterns = mutableListOf<CriticalPattern>()

        // PATRÓN 1: Burnout severo (triada de Maslach)
        val agotamientoSevero = q.agotamientoEmocional.contains("Constantemente") ||
                q.agotamientoEmocional.contains("Frecuentemente")
        val despersonalizacionSevera = q.despersonalizacion.contains("Constantemente") ||
                q.despersonalizacion.contains("Frecuentemente")
        val ineficaciaSevera = q.sentimientoIneficacia >= 4

        if (agotamientoSevero && despersonalizacionSevera && ineficaciaSevera) {
            patterns.add(CriticalPattern(
                area = "Burnout",
                severity = CriticalLevel.INTERVENCION_URGENTE,
                description = "Síndrome de burnout severo detectado",
                recommendation = "URGENTE: Consulta psicológica inmediata. Evaluar necesidad de baja laboral."
            ))
        }

        // PATRÓN 2: Nivel de estrés crítico
        if (q.nivelEstresGeneral >= 4) {
            patterns.add(CriticalPattern(
                area = "Estrés General",
                severity = CriticalLevel.ATENCION_REQUERIDA,
                description = "Nivel de estrés muy alto",
                recommendation = "Consulta con profesional de salud mental. Técnicas de manejo de estrés."
            ))
        }

        // PATRÓN 3: Interferencia severa con vida personal
        if (q.estresAfectaVidaPersonal.contains("Severamente")) {
            patterns.add(CriticalPattern(
                area = "Balance Vida-Trabajo",
                severity = CriticalLevel.ATENCION_REQUERIDA,
                description = "Estrés laboral afecta severamente la vida personal",
                recommendation = "Reevaluar carga laboral. Apoyo psicológico y establecimiento de límites."
            ))
        }

        return patterns
    }

    // ==================== CÁLCULOS DE SCORES ====================

    /**
     * 0. ✅ CORREGIDO: SALUD GENERAL con límite de condiciones médicas
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

        // ✅ CORREGIDO: Condiciones médicas con límite máximo
        val seriousConditions = listOf("Diabetes", "Hipertensión", "Problemas cardíacos",
            "Ansiedad/Depresión", "Hernia discal")
        val userConditions = q.preexistingConditions.filter { it in seriousConditions }
        val conditionsScore = (userConditions.size * 5).coerceAtMost(25) // ✅ Máximo 25 puntos
        score += conditionsScore

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

        // Indicadores (0-13 puntos)
        if (q.bloodPressure.contains("Hipertensión")) score += 5
        if (q.cholesterolLevel.contains("Alto")) score += 3
        if (q.bloodGlucose.contains("Diabetes")) score += 5

        // ✅ VALIDACIÓN ESTRICTA: Nunca superar 100
        score = score.coerceIn(0, 100)

        val risk = when {
            score < 20 -> RiskLevel.BAJO
            score < 40 -> RiskLevel.MODERADO
            score < 60 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Salud General: score=$score, risk=${risk.displayName}")

        return Pair(score, risk)
    }

    /**
     * 1. ERGONOMÍA (Score 0-100, mayor es mejor) ⚠️ ESCALA INVERTIDA
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

        // ✅ VALIDACIÓN ESTRICTA
        score = score.coerceIn(0, 100)

        // ⚠️ Clasificación INVERTIDA (alto score = bajo riesgo)
        val risk = when {
            score >= 80 -> RiskLevel.BAJO
            score >= 60 -> RiskLevel.MODERADO
            score >= 40 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Ergonomía: score=$score (ESCALA INVERTIDA), risk=${risk.displayName}")

        return Pair(score, risk)
    }

    /**
     * 2. ✅ MEJORADO: SÍNTOMAS MUSCULARES con normalización dinámica
     */
    fun calculateSintomasMuscularesScore(q: SintomasMuscularesQuestionnaire): Pair<Int, RiskLevel> {
        var totalSymptoms = 0.0
        var maxIntensity = 0

        // Cuello (peso: alto)
        val cuelloScore = (q.dolorCuelloFrecuencia + q.dolorCuelloIntensidad +
                q.rigidezCuelloFrecuencia + q.rigidezCuelloIntensidad) / 4.0
        totalSymptoms += (cuelloScore * 2)
        maxIntensity = maxOf(maxIntensity, q.dolorCuelloIntensidad, q.rigidezCuelloIntensidad)

        // Hombros (peso: alto)
        val hombrosScore = (q.dolorHombrosFrecuencia + q.dolorHombrosIntensidad) / 2.0
        totalSymptoms += (hombrosScore * 2)
        maxIntensity = maxOf(maxIntensity, q.dolorHombrosIntensidad)

        // Espalda (peso: muy alto)
        val espaldaScore = (q.dolorEspaldaAltaFrecuencia + q.dolorEspaldaAltaIntensidad +
                q.dolorEspaldaBajaFrecuencia + q.dolorEspaldaBajaIntensidad +
                q.rigidezEspaldaMañanaFrecuencia + q.rigidezEspaldaMañanaIntensidad) / 6.0
        totalSymptoms += (espaldaScore * 3)
        maxIntensity = maxOf(maxIntensity, q.dolorEspaldaAltaIntensidad, q.dolorEspaldaBajaIntensidad)

        // Manos/muñecas (peso: alto)
        val manosScore = (q.dolorMunecasFrecuencia + q.dolorMunecasIntensidad +
                q.dolorManosFrecuencia + q.dolorManosIntensidad +
                q.hormigueoManosFrecuencia + q.hormigueoManosIntensidad) / 6.0
        totalSymptoms += (manosScore * 2)

        if (q.hormigueoPorNoche.contains("despierta")) totalSymptoms += 5

        // Dolor de cabeza
        val cabezaScore = (q.dolorCabezaFrecuencia + q.dolorCabezaIntensidad) / 2.0
        totalSymptoms += cabezaScore

        // Impacto funcional
        if (q.dolorImpidenActividades.contains("frecuentemente")) totalSymptoms += 10
        else if (q.dolorImpidenActividades.contains("ocasionalmente")) totalSymptoms += 5

        // ✅ NORMALIZACIÓN DINÁMICA usando máximo teórico
        val maxPossible = MaxScores.getSintomasMuscularesMax()
        val score = ((totalSymptoms / maxPossible) * 100).toInt().coerceIn(0, 100)

        // ✅ Clasificación de riesgo con detección de patrones críticos
        val criticalPatterns = detectMusculoskeletalPatterns(q)
        val hasUrgentPattern = criticalPatterns.any { it.severity == CriticalLevel.INTERVENCION_URGENTE }

        val risk = if (hasUrgentPattern) {
            RiskLevel.MUY_ALTO // ✅ Patrones críticos anulan score bajo
        } else {
            when {
                score < 20 || maxIntensity <= 1 -> RiskLevel.BAJO
                score < 40 || maxIntensity <= 2 -> RiskLevel.MODERADO
                score < 60 || maxIntensity <= 3 -> RiskLevel.ALTO
                else -> RiskLevel.MUY_ALTO
            }
        }

        android.util.Log.d(TAG, """
            Síntomas Musculares: 
            - Total: $totalSymptoms/$maxPossible
            - Score normalizado: $score
            - Intensidad máxima: $maxIntensity
            - Patrones críticos: ${criticalPatterns.size}
            - Riesgo: ${risk.displayName}
        """.trimIndent())

        return Pair(score, risk)
    }

    /**
     * 3. ✅ MEJORADO: SÍNTOMAS VISUALES con normalización dinámica
     */
    fun calculateSintomasVisualesScore(q: SintomasVisualesQuestionnaire): Pair<Int, RiskLevel> {
        var totalSymptoms = 0.0

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

        // ✅ NORMALIZACIÓN DINÁMICA
        val maxPossible = MaxScores.getSintomasVisualesMax()
        val score = ((totalSymptoms / maxPossible) * 100).toInt().coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 50 -> RiskLevel.MODERADO
            score < 70 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Síntomas Visuales: $totalSymptoms/$maxPossible = $score (${risk.displayName})")

        return Pair(score, risk)
    }

    /**
     * 4. ✅ CORREGIDO: CARGA DE TRABAJO con normalización correcta
     */
    fun calculateCargaTrabajoScore(q: CargaTrabajoQuestionnaire): Pair<Int, RiskLevel> {
        var demandScore = 0
        var controlScore = 0
        var supportScore = 0

        // DEMANDA (máx 40)
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

        // CONTROL (máx 30)
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

        // APOYO (máx 30)
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

        // SATISFACCIÓN (máx 10)
        var satisfactionPenalty = 0
        if (q.satisfaccionGeneral.contains("Muy insatisfecho")) satisfactionPenalty += 5
        if (q.trabajoValorado.contains("Nunca") || q.trabajoValorado.contains("Rara vez")) {
            satisfactionPenalty += 5
        }

        val totalScore = demandScore + controlScore + supportScore + satisfactionPenalty

        // ✅ NORMALIZACIÓN CORRECTA usando máximo teórico
        val maxPossible = MaxScores.CARGA_TRABAJO_MAX.toDouble()
        val score = ((totalScore / maxPossible) * 100).toInt().coerceIn(0, 100)

        val risk = when {
            score < 30 -> RiskLevel.BAJO
            score < 50 -> RiskLevel.MODERADO
            score < 70 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Carga de Trabajo: $totalScore/$maxPossible = $score (${risk.displayName})")

        return Pair(score, risk)
    }

    /**
     * 5. ✅ CORREGIDO: ESTRÉS Y SALUD MENTAL con multiplicación correcta
     */
    fun calculateEstresSaludMentalScore(q: EstresSaludMentalQuestionnaire): Pair<Int, RiskLevel> {
        var score = 0.0 // ✅ Usar Double para cálculos precisos

        // ✅ CORRECCIÓN CRÍTICA: Multiplicación correcta
        score += (q.nivelEstresGeneral * 1.5) // ANTES: q.nivelEstresGeneral * 1.5.toInt() = *1

        if (q.estresAumento6Meses.contains("significativamente")) score += 5

        val stressSymptoms = listOf(
            q.fatigaAgotamiento, q.dificultadConcentracion, q.problemasMemoria,
            q.irritabilidad, q.ansiedadTrabajo, q.preocupacionesConstantes,
            q.sensacionAbrumado, q.dificultadDesconectar
        )
        val avgStressSymptoms = stressSymptoms.average()
        score += (avgStressSymptoms * 8)

        val burnoutSymptoms = listOf(
            q.perdidaMotivacion, q.sensacionInproductividad,
            q.actitudNegativa, q.sentimientoIneficacia
        )
        val avgBurnout = burnoutSymptoms.average()
        score += (avgBurnout * 8)

        if (q.agotamientoEmocional.contains("Constantemente")) score += 10
        else if (q.agotamientoEmocional.contains("Frecuentemente")) score += 7

        if (q.despersonalizacion.contains("Constantemente")) score += 10
        else if (q.despersonalizacion.contains("Frecuentemente")) score += 7

        if (q.estresAfectaVidaPersonal.contains("Severamente")) score += 10
        else if (q.estresAfectaVidaPersonal.contains("Significativamente")) score += 7

        // ✅ NORMALIZACIÓN DINÁMICA
        val maxPossible = MaxScores.getEstresMax()
        val normalizedScore = ((score / maxPossible) * 100).toInt().coerceIn(0, 100)

        // ✅ Detección de patrones críticos
        val criticalPatterns = detectStressPatterns(q)
        val hasUrgentPattern = criticalPatterns.any { it.severity == CriticalLevel.INTERVENCION_URGENTE }

        val risk = if (hasUrgentPattern) {
            RiskLevel.MUY_ALTO
        } else {
            when {
                normalizedScore < 25 -> RiskLevel.BAJO
                normalizedScore < 45 -> RiskLevel.MODERADO
                normalizedScore < 65 -> RiskLevel.ALTO
                else -> RiskLevel.MUY_ALTO
            }
        }

        android.util.Log.d(TAG, """
            Estrés: 
            - Total: $score/$maxPossible
            - Score normalizado: $normalizedScore
            - Patrones críticos: ${criticalPatterns.size}
            - Riesgo: ${risk.displayName}
        """.trimIndent())

        return Pair(normalizedScore, risk)
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

        // ✅ VALIDACIÓN ESTRICTA
        score = score.coerceIn(0, 100)

        val risk = when {
            score < 20 -> RiskLevel.BAJO
            score < 40 -> RiskLevel.MODERADO
            score < 60 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Hábitos de Sueño: score=$score (${risk.displayName})")

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

        // ✅ VALIDACIÓN ESTRICTA
        score = score.coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 45 -> RiskLevel.MODERADO
            score < 65 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Actividad Física: score=$score (${risk.displayName})")

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

        // ✅ VALIDACIÓN ESTRICTA
        score = score.coerceIn(0, 100)

        val risk = when {
            score < 25 -> RiskLevel.BAJO
            score < 45 -> RiskLevel.MODERADO
            score < 65 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, "Balance Vida-Trabajo: score=$score (${risk.displayName})")

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

        scores.forEach { (key, pair) ->
            val weight = weights[key] ?: 0.0
            weightedSum += pair.first * weight
            totalWeight += weight
        }

        val overallScore = if (totalWeight > 0) {
            (weightedSum / totalWeight).toInt()
        } else {
            0
        }

        // ✅ VALIDACIÓN ESTRICTA
        val validatedScore = overallScore.coerceIn(0, 100)

        val finalRisk = when {
            validatedScore < 25 -> RiskLevel.BAJO
            validatedScore < 50 -> RiskLevel.MODERADO
            validatedScore < 70 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        android.util.Log.d(TAG, """
            📊 Score Global:
            - Score ponderado: $validatedScore
            - Riesgo: ${finalRisk.displayName}
            - Áreas evaluadas: ${scores.size}/9
            - Pesos aplicados: ${weights.filter { it.key in scores.keys }}
        """.trimIndent())

        return Pair(validatedScore, finalRisk)
    }

    /**
     * ✅ NUEVO: Obtener todos los patrones críticos detectados
     */
    fun getAllCriticalPatterns(
        sintomasMusculares: SintomasMuscularesQuestionnaire?,
        estresSaludMental: EstresSaludMentalQuestionnaire?
    ): List<CriticalPattern> {
        val allPatterns = mutableListOf<CriticalPattern>()

        sintomasMusculares?.let {
            allPatterns.addAll(detectMusculoskeletalPatterns(it))
        }

        estresSaludMental?.let {
            allPatterns.addAll(detectStressPatterns(it))
        }

        return allPatterns
    }
}