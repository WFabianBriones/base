package com.example.uleammed

import kotlin.math.max
import kotlin.math.min

/**
 * Extractor de características desde los cuestionarios para el modelo de burnout
 */
class BurnoutFeatureExtractor {

    /**
     * Convierte todas las respuestas de cuestionarios a un objeto BurnoutInputData
     */
    fun extractFeatures(
        ergonomia: ErgonomiaQuestionnaire,
        sintomasMusculares: SintomasMuscularesQuestionnaire,
        sintomasVisuales: SintomasVisualesQuestionnaire,
        cargaTrabajo: CargaTrabajoQuestionnaire,
        estresSaludMental: EstresSaludMentalQuestionnaire,
        habitosSueno: HabitosSuenoQuestionnaire,
        actividadFisica: ActividadFisicaQuestionnaire,
        balanceVidaTrabajo: BalanceVidaTrabajoQuestionnaire,
        healthQuestionnaire: HealthQuestionnaire
    ): BurnoutInputData {

        return BurnoutInputData(
            // ERGONOMÍA
            scoreErgonomia = calculateErgonomiaScore(ergonomia),
            calidadSilla = normalizeSillaQuality(ergonomia.tipoSilla, ergonomia.soporteLumbar),
            calidadMonitor = normalizeMonitorQuality(ergonomia.tipoMonitor, ergonomia.alturaMonitor),
            calidadIluminacion = normalizeIluminacion(ergonomia.iluminacionPrincipal, ergonomia.reflejosPantalla),
            frecuenciaPausas = normalizePausas(ergonomia.pausasActivas, ergonomia.duracionPausas),

            // SÍNTOMAS MÚSCULO-ESQUELÉTICOS
            dolorCuello = normalizeScale(sintomasMusculares.dolorCuelloIntensidad),
            dolorEspalda = normalizeScale((sintomasMusculares.dolorEspaldaAltaIntensidad +
                    sintomasMusculares.dolorEspaldaBajaIntensidad) / 2f),
            dolorHombros = normalizeScale(sintomasMusculares.dolorHombrosIntensidad),
            dolorMunecas = normalizeScale(sintomasMusculares.dolorMunecasIntensidad),
            rigidezMuscular = normalizeScale((sintomasMusculares.rigidezCuelloIntensidad +
                    sintomasMusculares.rigidezEspaldaMañanaIntensidad) / 2f),
            limitacionMovimiento = normalizeBoolean(sintomasMusculares.dolorImpidenActividades),

            // SÍNTOMAS VISUALES
            fatigaVisual = normalizeScale((sintomasVisuales.ojosSecosIntensidad +
                    sintomasVisuales.ardorOjosIntensidad) / 2f),
            ojosSecos = normalizeScale(sintomasVisuales.ojosSecosFrecuencia),
            visionBorrosa = normalizeScale(sintomasVisuales.visionBorrosaFrecuencia),
            sensibilidadLuz = normalizeScale(sintomasVisuales.sensibilidadLuzFrecuencia),

            // CARGA DE TRABAJO - MUY IMPORTANTE
            cargaTrabajo = normalizeCargaTrabajo(cargaTrabajo.cargaTrabajoActual),
            presionTiempo = normalizePresion(cargaTrabajo.presionTiempoPlazos),
            horasExtra = normalizeHorasExtra(cargaTrabajo.horasFueraHorario),
            trabajoFinSemana = normalizeBoolean(cargaTrabajo.trabajaFinesSemana),
            autonomia = normalizeAutonomia(cargaTrabajo.puedeDecidirComoTrabajar),
            apoyoSocial = normalizeApoyo(cargaTrabajo.apoyoSuperior, cargaTrabajo.relacionCompaneros),
            satisfaccionLaboral = normalizeSatisfaccion(cargaTrabajo.satisfaccionGeneral),

            // ESTRÉS Y SALUD MENTAL - EL MÁS IMPORTANTE
            nivelEstres = estresSaludMental.nivelEstresGeneral / 10f,
            fatigaEmocional = normalizeScale(estresSaludMental.fatigaAgotamiento),
            dificultadConcentracion = normalizeScale(estresSaludMental.dificultadConcentracion),
            irritabilidad = normalizeScale(estresSaludMental.irritabilidad),
            ansiedad = normalizeScale(estresSaludMental.ansiedadTrabajo),
            perdidaMotivacion = normalizeScale(estresSaludMental.perdidaMotivacion),
            sensacionAbrumado = normalizeScale(estresSaludMental.sensacionAbrumado),
            despersonalizacion = normalizeDespersonalizacion(estresSaludMental.despersonalizacion),
            agotamientoEmocional = normalizeAgotamiento(estresSaludMental.agotamientoEmocional),

            // HÁBITOS DE SUEÑO
            calidadSueno = normalizeCalidadSueno(habitosSueno.calidadSueno),
            horasSueno = normalizeHorasSueno(habitosSueno.horasSuenoSemana),
            dificultadDormir = normalizeScale(habitosSueno.dificultadConciliarFrecuencia),

            // ACTIVIDAD FÍSICA
            frecuenciaEjercicio = normalizeFrecuenciaEjercicio(actividadFisica.frecuenciaEjercicio),
            nivelActividad = normalizeNivelActividad(actividadFisica.duracionEjercicio),

            // BALANCE VIDA-TRABAJO
            equilibrioVidaTrabajo = normalizeEquilibrio(balanceVidaTrabajo.equilibrioTrabajoVida),
            tiempoLibre = normalizeTiempoLibre(balanceVidaTrabajo.tiempoLibreCalidad),
            capacidadDesconectar = normalizeDesconexion(balanceVidaTrabajo.puedeDesconectarseDiasLibres),

            // DEMOGRÁFICAS
            edad = normalizeEdad(healthQuestionnaire.ageRange),
            aniosExperiencia = normalizeExperiencia(healthQuestionnaire.ageRange),
            genero = normalizeGenero(healthQuestionnaire.gender)
        )
    }

    // ============ FUNCIONES DE NORMALIZACIÓN ============

    private fun normalizeScale(value: Int): Float = min(value / 5f, 1f)

    private fun calculateErgonomiaScore(e: ErgonomiaQuestionnaire): Float {
        var score = 0f
        var count = 0

        // Silla ergonómica
        score += when {
            e.tipoSilla.contains("ergonómica ajustable") -> 1f
            e.tipoSilla.contains("ajuste de altura y respaldo") -> 0.75f
            e.tipoSilla.contains("ajuste de altura solamente") -> 0.5f
            e.tipoSilla.contains("básica") -> 0.25f
            else -> 0f
        }
        count++

        // Monitor
        score += when {
            e.alturaMonitor.contains("altura de los ojos") -> 1f
            e.alturaMonitor.contains("10-15cm por debajo") -> 0.75f
            e.alturaMonitor.contains("Por encima") -> 0.3f
            else -> 0.5f
        }
        count++

        // Pausas
        score += when {
            e.pausasActivas.contains("30-60 minutos") -> 1f
            e.pausasActivas.contains("1-2 horas") -> 0.7f
            e.pausasActivas.contains("3-4 horas") -> 0.4f
            else -> 0.2f
        }
        count++

        return score / count
    }

    private fun normalizeSillaQuality(tipoSilla: String, soporteLumbar: String): Float {
        val sillaScore = when {
            tipoSilla.contains("ergonómica ajustable") -> 1f
            tipoSilla.contains("ajuste de altura y respaldo") -> 0.75f
            tipoSilla.contains("ajuste de altura solamente") -> 0.5f
            tipoSilla.contains("básica") -> 0.25f
            else -> 0f
        }

        val lumbarScore = when {
            soporteLumbar.contains("ajustable") -> 1f
            soporteLumbar.contains("fijo") -> 0.6f
            else -> 0f
        }

        return (sillaScore + lumbarScore) / 2f
    }

    private fun normalizeMonitorQuality(tipoMonitor: String, alturaMonitor: String): Float {
        val tipoScore = when {
            tipoMonitor.contains("Monitor externo") && !tipoMonitor.contains("Laptop") -> 1f
            tipoMonitor.contains("Laptop con monitor externo") -> 0.8f
            tipoMonitor.contains("laptop (con soporte)") -> 0.5f
            tipoMonitor.contains("laptop (sin soporte)") -> 0.2f
            else -> 0.1f
        }

        val alturaScore = when {
            alturaMonitor.contains("altura de los ojos") -> 1f
            alturaMonitor.contains("10-15cm por debajo") -> 0.8f
            alturaMonitor.contains("Por encima") -> 0.3f
            alturaMonitor.contains("Más de 15cm por debajo") -> 0.4f
            else -> 0.5f
        }

        return (tipoScore + alturaScore) / 2f
    }

    private fun normalizeIluminacion(iluminacion: String, reflejos: String): Float {
        val ilumScore = when {
            iluminacion.contains("natural abundante") -> 1f
            iluminacion.contains("natural moderada") -> 0.8f
            iluminacion.contains("Mezcla") -> 0.7f
            iluminacion.contains("artificial") -> 0.6f
            else -> 0.3f
        }

        val reflejoScore = when {
            reflejos.contains("Nunca") -> 1f
            reflejos.contains("Ocasionalmente") -> 0.7f
            reflejos.contains("Frecuentemente") -> 0.4f
            else -> 0.2f
        }

        return (ilumScore + reflejoScore) / 2f
    }

    private fun normalizePausas(pausas: String, duracion: String): Float {
        val pausaScore = when {
            pausas.contains("30-60 minutos") -> 1f
            pausas.contains("1-2 horas") -> 0.7f
            pausas.contains("3-4 horas") -> 0.4f
            pausas.contains("baño") -> 0.2f
            else -> 0f
        }

        val duracionScore = when {
            duracion.contains("5-10 minutos") -> 1f
            duracion.contains("2-5 minutos") -> 0.6f
            duracion.contains("Menos de 2") -> 0.3f
            else -> 0f
        }

        return (pausaScore + duracionScore) / 2f
    }

    private fun normalizeBoolean(value: String): Float {
        return when {
            value.contains("Sí") || value.contains("Frecuentemente") ||
                    value.contains("Significativamente") -> 1f
            value.contains("veces") || value.contains("Moderadamente") -> 0.6f
            value.contains("poco") || value.contains("Ocasionalmente") -> 0.3f
            else -> 0f
        }
    }

    private fun normalizeCargaTrabajo(carga: String): Float = when {
        carga.contains("Excesiva") -> 1f
        carga.contains("Alta") -> 0.75f
        carga.contains("Adecuada") -> 0.5f
        carga.contains("Baja") -> 0.25f
        else -> 0.1f
    }

    private fun normalizePresion(presion: String): Float = when {
        presion.contains("Muy alta") || presion.contains("Constante") -> 1f
        presion.contains("Alta") -> 0.75f
        presion.contains("Moderada") -> 0.5f
        presion.contains("Baja") -> 0.25f
        else -> 0f
    }

    private fun normalizeHorasExtra(horas: String): Float = when {
        horas.contains("Más de 12") -> 1f
        horas.contains("8-12") -> 0.75f
        horas.contains("4-7") -> 0.5f
        horas.contains("1-3") -> 0.25f
        else -> 0f
    }

    private fun normalizeAutonomia(autonomia: String): Float = when {
        autonomia.contains("total autonomía") -> 1f
        autonomia.contains("mayoría") -> 0.75f
        autonomia.contains("Parcialmente") -> 0.5f
        autonomia.contains("Poco") -> 0.25f
        else -> 0f
    }

    private fun normalizeApoyo(superior: String, companeros: String): Float {
        val supScore = when {
            superior.contains("Muy bueno") -> 1f
            superior.contains("Bueno") -> 0.75f
            superior.contains("Regular") -> 0.5f
            superior.contains("Malo") -> 0.25f
            else -> 0f
        }

        val compScore = when {
            companeros.contains("Excelente") -> 1f
            companeros.contains("Buena") -> 0.75f
            companeros.contains("Regular") -> 0.5f
            companeros.contains("Mala") -> 0.25f
            else -> 0.5f
        }

        return (supScore + compScore) / 2f
    }

    private fun normalizeSatisfaccion(satisfaccion: String): Float = when {
        satisfaccion.contains("Muy satisfecho") -> 1f
        satisfaccion.contains("Satisfecho") -> 0.75f
        satisfaccion.contains("Neutral") -> 0.5f
        satisfaccion.contains("Insatisfecho") -> 0.25f
        else -> 0f
    }

    private fun normalizeDespersonalizacion(value: String): Float = when {
        value.contains("Constantemente") -> 1f
        value.contains("Frecuentemente") -> 0.75f
        value.contains("Ocasionalmente") -> 0.5f
        value.contains("Rara vez") -> 0.25f
        else -> 0f
    }

    private fun normalizeAgotamiento(value: String): Float = when {
        value.contains("Constantemente") || value.contains("Siempre") -> 1f
        value.contains("Frecuentemente") -> 0.75f
        value.contains("Ocasionalmente") -> 0.5f
        value.contains("Rara vez") -> 0.25f
        else -> 0f
    }

    private fun normalizeCalidadSueno(calidad: String): Float = when {
        calidad.contains("Excelente") -> 1f
        calidad.contains("Buena") -> 0.75f
        calidad.contains("Regular") -> 0.5f
        calidad.contains("Mala") -> 0.25f
        else -> 0.1f
    }

    private fun normalizeHorasSueno(horas: String): Float = when {
        horas.contains("7-8") -> 1f
        horas.contains("Más de 8") -> 0.9f
        horas.contains("5-6") -> 0.5f
        horas.contains("Menos de 5") -> 0.2f
        else -> 0.5f
    }

    private fun normalizeFrecuenciaEjercicio(frecuencia: String): Float = when {
        frecuencia.contains("Diariamente") -> 1f
        frecuencia.contains("4-5 veces") -> 0.8f
        frecuencia.contains("2-3 veces") -> 0.6f
        frecuencia.contains("1 vez") -> 0.3f
        else -> 0f
    }

    private fun normalizeNivelActividad(duracion: String): Float = when {
        duracion.contains("Más de 60") -> 1f
        duracion.contains("30-60") -> 0.75f
        duracion.contains("20-30") -> 0.5f
        duracion.contains("Menos de 20") -> 0.25f
        else -> 0f
    }

    private fun normalizeEquilibrio(equilibrio: String): Float = when {
        equilibrio.contains("excelente") -> 1f
        equilibrio.contains("buen balance") -> 0.75f
        equilibrio.contains("Parcialmente") -> 0.5f
        equilibrio.contains("Más trabajo") -> 0.25f
        else -> 0f
    }

    private fun normalizeTiempoLibre(tiempo: String): Float = when {
        tiempo.contains("Más de 20") -> 1f
        tiempo.contains("15-20") -> 0.75f
        tiempo.contains("10-15") -> 0.5f
        tiempo.contains("5-10") -> 0.25f
        else -> 0.1f
    }

    private fun normalizeDesconexion(desconexion: String): Float = when {
        desconexion.contains("completamente") -> 1f
        desconexion.contains("Mayormente") -> 0.75f
        desconexion.contains("dificultad") -> 0.5f
        desconexion.contains("Rara vez") -> 0.25f
        else -> 0f
    }

    private fun normalizeEdad(ageRange: String): Float = when {
        ageRange.contains("18-25") -> 0.2f
        ageRange.contains("26-35") -> 0.4f
        ageRange.contains("36-45") -> 0.6f
        ageRange.contains("46-55") -> 0.8f
        else -> 1f
    }

    private fun normalizeExperiencia(ageRange: String): Float {
        // Aproximación: experiencia correlacionada con edad
        return when {
            ageRange.contains("18-25") -> 0.1f
            ageRange.contains("26-35") -> 0.3f
            ageRange.contains("36-45") -> 0.6f
            ageRange.contains("46-55") -> 0.8f
            else -> 1f
        }
    }

    private fun normalizeGenero(gender: String): Float = when {
        gender.contains("Femenino") -> 0f
        gender.contains("Masculino") -> 1f
        else -> 0.5f
    }
}