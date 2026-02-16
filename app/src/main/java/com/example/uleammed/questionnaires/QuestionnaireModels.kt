package com.example.uleammed.questionnaires

import androidx.compose.ui.graphics.vector.ImageVector

// ✅ ACTUALIZADO: Agregado SALUD_GENERAL
enum class QuestionnaireType {
    SALUD_GENERAL,          // ✅ NUEVO
    ERGONOMIA,
    SINTOMAS_MUSCULARES,
    SINTOMAS_VISUALES,
    CARGA_TRABAJO,
    ESTRES_SALUD_MENTAL,
    HABITOS_SUENO,
    ACTIVIDAD_FISICA,
    BALANCE_VIDA_TRABAJO
}

// Información del cuestionario
data class QuestionnaireInfo(
    val type: QuestionnaireType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val estimatedTime: String,
    val totalQuestions: Int
)

// ✅ NUEVO: Cuestionario de Salud General (SF-12)
// ✅ NUEVO: Cuestionario de Salud General (SF-12)
data class SaludGeneralQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",

    // Salud General (1 pregunta)
    val saludGeneral: String = "", // Excelente/Muy buena/Buena/Regular/Mala

    // Limitaciones por salud física (2 preguntas)
    val limitacionActividadesModeradas: String = "", // Sí mucho/Sí un poco/No
    val limitacionSubirEscaleras: String = "", // Sí mucho/Sí un poco/No

    // Limitaciones por problemas emocionales (2 preguntas)
    val problemasEmocionales: String = "", // Sí/No
    val menosRendimientoEmocional: String = "", // Sí/No

    // Dolor (1 pregunta)
    val dolorInterfiereActividadesNormales: String = "", // Nada/Un poco/Moderado/Bastante/Mucho

    // Salud Mental (3 preguntas)
    val frecuenciaCalma: String = "", // Siempre/Casi siempre/Muchas veces/Algunas veces/Solo alguna vez/Nunca
    val frecuenciaEnergia: String = "", // Siempre/Casi siempre/Muchas veces/Algunas veces/Solo alguna vez/Nunca
    val frecuenciaDesanimado: String = "", // Siempre/Casi siempre/Muchas veces/Algunas veces/Solo alguna vez/Nunca

    // Función Social (1 pregunta)
    val saludInterfiereActividadesSociales: String = "", // Siempre/Casi siempre/Algunas veces/Solo alguna vez/Nunca

    val completedAt: Long = 0L
)

// 1. Ergonomía y Ambiente de Trabajo
data class ErgonomiaQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val tipoSilla: String = "",
    val soporteLumbar: String = "",
    val alturaEscritorio: String = "",
    val espacioEscritorio: String = "",
    val tipoMonitor: String = "",
    val alturaMonitor: String = "",
    val distanciaMonitor: String = "",
    val usaMasDeUnMonitor: String = "",
    val posicionTeclado: String = "",
    val tipoMouse: String = "",
    val usaAlmohadilla: String = "",
    val iluminacionPrincipal: String = "",
    val reflejosPantalla: String = "",
    val lamparaEscritorio: String = "",
    val temperatura: String = "",
    val nivelRuido: String = "",
    val ventilacion: String = "",
    val pausasActivas: String = "",
    val duracionPausas: String = "",
    val realizaEstiramientos: String = "",
    val tiempoSentadoContinuo: String = "",
    val completedAt: Long = 0L
)

// 2. Síntomas Músculo-Esqueléticos
data class SintomasMuscularesQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val dolorCuelloFrecuencia: Int = 0,
    val dolorCuelloIntensidad: Int = 0,
    val rigidezCuelloFrecuencia: Int = 0,
    val rigidezCuelloIntensidad: Int = 0,
    val dolorHombrosFrecuencia: Int = 0,
    val dolorHombrosIntensidad: Int = 0,
    val dolorAumentaFinDia: String = "",
    val dolorEspaldaAltaFrecuencia: Int = 0,
    val dolorEspaldaAltaIntensidad: Int = 0,
    val dolorEspaldaBajaFrecuencia: Int = 0,
    val dolorEspaldaBajaIntensidad: Int = 0,
    val rigidezEspaldaMañanaFrecuencia: Int = 0,
    val rigidezEspaldaMañanaIntensidad: Int = 0,
    val dolorConMovimiento: String = "",
    val dolorMunecasFrecuencia: Int = 0,
    val dolorMunecasIntensidad: Int = 0,
    val dolorManosFrecuencia: Int = 0,
    val dolorManosIntensidad: Int = 0,
    val hormigueoManosFrecuencia: Int = 0,
    val hormigueoManosIntensidad: Int = 0,
    val hormigueoPorNoche: String = "",
    val dolorCodosFrecuencia: Int = 0,
    val dolorCodosIntensidad: Int = 0,
    val debilidadAgarrar: Int = 0,
    val dolorCabezaFrecuencia: Int = 0,
    val dolorCabezaIntensidad: Int = 0,
    val momentoDolorCabeza: String = "",
    val dolorImpidenActividades: String = "",
    val haConsultadoMedico: String = "",
    val completedAt: Long = 0L
)

// 3. Síntomas Visuales
data class SintomasVisualesQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val ojosSecosFrecuencia: Int = 0,
    val ojosSecosIntensidad: Int = 0,
    val ardorOjosFrecuencia: Int = 0,
    val ardorOjosIntensidad: Int = 0,
    val ojosRojosFrecuencia: Int = 0,
    val lagrimeoFrecuencia: Int = 0,
    val visionBorrosaFrecuencia: Int = 0,
    val dificultadEnfocarFrecuencia: Int = 0,
    val sensibilidadLuzFrecuencia: Int = 0,
    val visionDobleFrecuencia: Int = 0,
    val ojosCansadosFinDia: String = "",
    val esfuerzoVerNitidamente: String = "",
    val usaLentes: String = "",
    val ultimoExamenVisual: String = "",
    val aplicaRegla202020: String = "",
    val brilloPantalla: String = "",
    val completedAt: Long = 0L
)

// 4. Carga de Trabajo y Factores Psicosociales
data class CargaTrabajoQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val cargaTrabajoActual: String = "",
    val presionTiempoPlazos: String = "",
    val ritmoTrabajo: String = "",
    val llevaTrabajoCasa: String = "",
    val trabajaFinesSemana: String = "",
    val horasFueraHorario: String = "",
    val puedeDecidirComoTrabajar: String = "",
    val puedePlanificarPausas: String = "",
    val participaDecisiones: String = "",
    val apoyoSuperior: String = "",
    val relacionCompaneros: String = "",
    val acosoLaboral: String = "",
    val trabajoValorado: String = "",
    val satisfaccionGeneral: String = "",
    val salarioAdecuado: String = "",
    val completedAt: Long = 0L
)

// 5. Estrés y Salud Mental
data class EstresSaludMentalQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val nivelEstresGeneral: Int = 0,
    val estresAumento6Meses: String = "",
    val fatigaAgotamiento: Int = 0,
    val dificultadConcentracion: Int = 0,
    val problemasMemoria: Int = 0,
    val irritabilidad: Int = 0,
    val ansiedadTrabajo: Int = 0,
    val preocupacionesConstantes: Int = 0,
    val sensacionAbrumado: Int = 0,
    val dificultadDesconectar: Int = 0,
    val perdidaMotivacion: Int = 0,
    val sensacionInproductividad: Int = 0,
    val actitudNegativa: Int = 0,
    val sentimientoIneficacia: Int = 0,
    val agotamientoEmocional: String = "",
    val despersonalizacion: String = "",
    val estresAfectaVidaPersonal: String = "",
    val consideraCambiarTrabajo: String = "",
    val trabajoInterfiereTiempoDescanso: String = "",
    val completedAt: Long = 0L
)

// 6. Hábitos de Sueño
data class HabitosSuenoQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val horasSuenoSemana: String = "",
    val horasSuenoFinSemana: String = "",
    val calidadSueno: String = "",
    val dificultadConciliarFrecuencia: Int = 0,
    val despertaresNocturnosFrecuencia: Int = 0,
    val despertarTempranoFrecuencia: Int = 0,
    val usaDispositivosAntesDormir: String = "",
    val piensaProblemasTrabajoAntesDormir: String = "",
    val revisaCorreosFueraHorario: String = "",
    val completedAt: Long = 0L
)

// 7. Actividad Física
data class ActividadFisicaQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val frecuenciaEjercicio: String = "",
    val duracionEjercicio: String = "",
    val tipoActividadPrincipal: String = "",
    val realizaEstiramientos: String = "",
    val frecuenciaComidasDia: String = "",
    val saltaDesayuno: String = "",
    val comeEnEscritorio: String = "",
    val consumoAguaDiario: String = "",
    val consumoCafeTe: String = "",
    val consumeBebidasEnergizantes: String = "",
    val completedAt: Long = 0L
)

// 8. Balance Vida-Trabajo
data class BalanceVidaTrabajoQuestionnaire(
    val responseId: String = System.currentTimeMillis().toString(),
    val userId: String = "",
    val equilibrioTrabajoVida: String = "",
    val tiempoLibreCalidad: String = "",
    val actividadesRecreativas: String = "",
    val trabajoAfectaRelaciones: String = "",
    val tiempoFamiliaAmigos: String = "",
    val puedeDesconectarseDiasLibres: String = "",
    val revisaCorreosVacaciones: String = "",
    val ultimasVacaciones: String = "",
    val completedAt: Long = 0L
)