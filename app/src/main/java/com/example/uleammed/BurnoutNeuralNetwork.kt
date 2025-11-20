package com.example.uleammed

import android.content.Context
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.layer.regularization.Dropout
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import java.io.File
import kotlin.math.exp

/**
 * Red Neuronal para predicción de Burnout
 *
 * Arquitectura:
 * - Input: 42 características
 * - Hidden Layer 1: 128 neuronas + Dropout(0.3)
 * - Hidden Layer 2: 64 neuronas + Dropout(0.3)
 * - Hidden Layer 3: 32 neuronas + Dropout(0.2)
 * - Output: 3 neuronas (Bajo, Moderado, Alto) con Softmax
 */
class BurnoutNeuralNetwork(private val context: Context) {

    companion object {
        private const val INPUT_SIZE = 42
        private const val OUTPUT_SIZE = 3 // Bajo, Moderado, Alto
        private const val MODEL_FILE = "burnout_model.h5"
        private const val TAG = "BurnoutNN"
    }

    private var model: Sequential? = null
    private var isModelTrained = false

    /**
     * Inicializa la arquitectura de la red neuronal
     */
    fun initializeModel() {
        model = Sequential.of(
            // Capa de entrada
            Input(INPUT_SIZE),

            // Primera capa oculta: 128 neuronas
            Dense(
                outputSize = 128,
                activation = Activations.Relu,
                kernelInitializer = HeNormal(),
                name = "dense_1"
            ),
            Dropout(
                keepProbability = 0.7f, // 30% dropout
                name = "dropout_1"
            ),

            // Segunda capa oculta: 64 neuronas
            Dense(
                outputSize = 64,
                activation = Activations.Relu,
                kernelInitializer = HeNormal(),
                name = "dense_2"
            ),
            Dropout(
                keepProbability = 0.7f,
                name = "dropout_2"
            ),

            // Tercera capa oculta: 32 neuronas
            Dense(
                outputSize = 32,
                activation = Activations.Relu,
                kernelInitializer = HeNormal(),
                name = "dense_3"
            ),
            Dropout(
                keepProbability = 0.8f, // 20% dropout
                name = "dropout_3"
            ),

            // Capa de salida: 3 clases con Softmax
            Dense(
                outputSize = OUTPUT_SIZE,
                activation = Activations.Softmax,
                kernelInitializer = HeNormal(),
                name = "output"
            )
        )

        android.util.Log.d(TAG, "✅ Arquitectura del modelo inicializada")
    }

    /**
     * Compila el modelo con optimizador y función de pérdida
     */
    fun compileModel() {
        model?.compile(
            optimizer = Adam(
                learningRate = 0.001f,
                beta1 = 0.9f,
                beta2 = 0.999f
            ),
            loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
            metric = Metrics.ACCURACY
        )

        android.util.Log.d(TAG, "✅ Modelo compilado")
    }

    /**
     * Entrena el modelo con datos de ejemplo
     * En producción, deberías entrenar con datos reales recolectados
     */
    fun trainModel(
        trainingData: List<BurnoutInputData>,
        labels: List<Int>, // 0=Bajo, 1=Moderado, 2=Alto
        epochs: Int = 50,
        batchSize: Int = 32,
        validationSplit: Float = 0.2f
    ) {
        require(trainingData.size == labels.size) {
            "Training data y labels deben tener el mismo tamaño"
        }
        require(trainingData.isNotEmpty()) {
            "Training data no puede estar vacío"
        }

        android.util.Log.d(TAG, """
            🎯 Iniciando entrenamiento
            - Muestras: ${trainingData.size}
            - Épocas: $epochs
            - Batch size: $batchSize
            - Validación: ${(validationSplit * 100).toInt()}%
        """.trimIndent())

        // Convertir datos a arrays
        val xTrain = trainingData.map { it.toFloatArray() }.toTypedArray()
        val yTrain = labels.map { oneHotEncode(it) }.toTypedArray()

        // Crear dataset
        val dataset = OnHeapDataset.create(xTrain, yTrain)

        // Entrenar
        model?.fit(
            dataset = dataset,
            epochs = epochs,
            batchSize = batchSize,
            verbose = true
        )

        isModelTrained = true

        android.util.Log.d(TAG, "✅ Entrenamiento completado")
    }

    /**
     * Predice el nivel de burnout para un conjunto de características
     */
    fun predict(inputData: BurnoutInputData): BurnoutPrediction {
        require(isModelTrained || modelExists()) {
            "El modelo debe estar entrenado o cargado antes de predecir"
        }

        // Si el modelo no está en memoria, cargarlo
        if (model == null && modelExists()) {
            loadModel()
        }

        val input = inputData.toFloatArray()

        // Realizar predicción
        val predictions = model?.predict(input) ?: floatArrayOf(0.33f, 0.33f, 0.34f)

        val probBajo = predictions[0]
        val probModerado = predictions[1]
        val probAlto = predictions[2]

        // Determinar nivel de riesgo
        val (nivelRiesgo, probabilidad) = when {
            probAlto >= 0.7f -> NivelRiesgo.CRITICO to probAlto
            probAlto >= 0.5f -> NivelRiesgo.ALTO to probAlto
            probModerado >= 0.5f -> NivelRiesgo.MODERADO to probModerado
            else -> NivelRiesgo.BAJO to probBajo
        }

        // Identificar factores de riesgo
        val factoresRiesgo = identifyRiskFactors(inputData)

        // Generar recomendaciones
        val recomendaciones = generateRecommendations(factoresRiesgo, nivelRiesgo)

        android.util.Log.d(TAG, """
            🔮 Predicción realizada
            - Nivel: ${nivelRiesgo.displayName}
            - Probabilidad: ${(probabilidad * 100).toInt()}%
            - Factores de riesgo: ${factoresRiesgo.size}
        """.trimIndent())

        return BurnoutPrediction(
            nivelRiesgo = nivelRiesgo,
            probabilidadBurnout = probabilidad,
            probabilidadBajo = probBajo,
            probabilidadModerado = probModerado,
            probabilidadAlto = probAlto,
            factoresRiesgo = factoresRiesgo,
            recomendaciones = recomendaciones
        )
    }

    /**
     * Identifica factores de riesgo basándose en los valores de entrada
     */
    private fun identifyRiskFactors(input: BurnoutInputData): List<FactorRiesgo> {
        val factores = mutableListOf<FactorRiesgo>()

        // ESTRÉS Y SALUD MENTAL (MÁS IMPORTANTE)
        if (input.nivelEstres >= 0.7f) {
            factores.add(FactorRiesgo(
                nombre = "Nivel de Estrés Elevado",
                categoria = CategoriaFactor.ESTRES_MENTAL,
                valor = input.nivelEstres,
                impacto = when {
                    input.nivelEstres >= 0.9f -> ImpactoFactor.CRITICO
                    input.nivelEstres >= 0.7f -> ImpactoFactor.ALTO
                    else -> ImpactoFactor.MODERADO
                },
                descripcion = "Tu nivel de estrés es ${(input.nivelEstres * 10).toInt()}/10"
            ))
        }

        if (input.agotamientoEmocional >= 0.6f) {
            factores.add(FactorRiesgo(
                nombre = "Agotamiento Emocional",
                categoria = CategoriaFactor.ESTRES_MENTAL,
                valor = input.agotamientoEmocional,
                impacto = if (input.agotamientoEmocional >= 0.8f) ImpactoFactor.CRITICO else ImpactoFactor.ALTO,
                descripcion = "Presentas signos de agotamiento emocional significativo"
            ))
        }

        if (input.perdidaMotivacion >= 0.7f) {
            factores.add(FactorRiesgo(
                nombre = "Pérdida de Motivación",
                categoria = CategoriaFactor.ESTRES_MENTAL,
                valor = input.perdidaMotivacion,
                impacto = ImpactoFactor.ALTO,
                descripcion = "Has perdido interés y motivación por tu trabajo"
            ))
        }

        // CARGA DE TRABAJO
        if (input.cargaTrabajo >= 0.7f) {
            factores.add(FactorRiesgo(
                nombre = "Sobrecarga Laboral",
                categoria = CategoriaFactor.CARGA_TRABAJO,
                valor = input.cargaTrabajo,
                impacto = ImpactoFactor.ALTO,
                descripcion = "Tu carga de trabajo es excesiva"
            ))
        }

        if (input.horasExtra >= 0.6f) {
            factores.add(FactorRiesgo(
                nombre = "Exceso de Horas Extra",
                categoria = CategoriaFactor.CARGA_TRABAJO,
                valor = input.horasExtra,
                impacto = ImpactoFactor.MODERADO,
                descripcion = "Trabajas demasiadas horas fuera de tu horario"
            ))
        }

        if (input.apoyoSocial <= 0.4f) {
            factores.add(FactorRiesgo(
                nombre = "Falta de Apoyo Social",
                categoria = CategoriaFactor.CARGA_TRABAJO,
                valor = 1f - input.apoyoSocial,
                impacto = ImpactoFactor.ALTO,
                descripcion = "No cuentas con suficiente apoyo de superiores o colegas"
            ))
        }

        // SUEÑO
        if (input.calidadSueno <= 0.4f || input.horasSueno <= 0.5f) {
            factores.add(FactorRiesgo(
                nombre = "Problemas de Sueño",
                categoria = CategoriaFactor.SUENO,
                valor = 1f - ((input.calidadSueno + input.horasSueno) / 2f),
                impacto = ImpactoFactor.ALTO,
                descripcion = "No duermes lo suficiente o tu sueño no es reparador"
            ))
        }

        // BALANCE VIDA-TRABAJO
        if (input.equilibrioVidaTrabajo <= 0.3f) {
            factores.add(FactorRiesgo(
                nombre = "Desequilibrio Vida-Trabajo",
                categoria = CategoriaFactor.BALANCE_VIDA,
                valor = 1f - input.equilibrioVidaTrabajo,
                impacto = ImpactoFactor.ALTO,
                descripcion = "Tu vida personal está siendo afectada por el trabajo"
            ))
        }

        // SÍNTOMAS FÍSICOS
        val dolorPromedio = (input.dolorCuello + input.dolorEspalda +
                input.dolorHombros + input.dolorMunecas) / 4f
        if (dolorPromedio >= 0.6f) {
            factores.add(FactorRiesgo(
                nombre = "Dolor Músculo-Esquelético",
                categoria = CategoriaFactor.SINTOMAS_FISICOS,
                valor = dolorPromedio,
                impacto = ImpactoFactor.MODERADO,
                descripcion = "Presentas dolor significativo en múltiples áreas"
            ))
        }

        // ACTIVIDAD FÍSICA
        if (input.frecuenciaEjercicio <= 0.3f) {
            factores.add(FactorRiesgo(
                nombre = "Sedentarismo",
                categoria = CategoriaFactor.ACTIVIDAD_FISICA,
                valor = 1f - input.frecuenciaEjercicio,
                impacto = ImpactoFactor.MODERADO,
                descripcion = "Realizas poca o nula actividad física"
            ))
        }

        // Ordenar por impacto
        return factores.sortedByDescending { it.impacto.ordinal }
    }

    /**
     * Genera recomendaciones personalizadas basadas en factores de riesgo
     */
    private fun generateRecommendations(
        factores: List<FactorRiesgo>,
        nivelRiesgo: NivelRiesgo
    ): List<Recomendacion> {
        val recomendaciones = mutableListOf<Recomendacion>()

        // Recomendación urgente si es crítico
        if (nivelRiesgo == NivelRiesgo.CRITICO) {
            recomendaciones.add(Recomendacion(
                titulo = "⚠️ Busca Ayuda Profesional Urgente",
                descripcion = "Tu nivel de burnout es crítico. Es fundamental que consultes con un profesional de salud mental.",
                prioridad = PrioridadRecomendacion.URGENTE,
                categoria = CategoriaFactor.ESTRES_MENTAL,
                accionesConcretas = listOf(
                    "Contacta al departamento de recursos humanos",
                    "Solicita una cita con un psicólogo o psiquiatra",
                    "Considera tomar licencia médica temporal",
                    "Habla con tu supervisor sobre tu situación"
                )
            ))
        }

        // Recomendaciones basadas en factores identificados
        factores.groupBy { it.categoria }.forEach { (categoria, factoresCategoria) ->
            when (categoria) {
                CategoriaFactor.ESTRES_MENTAL -> {
                    recomendaciones.add(Recomendacion(
                        titulo = "Gestiona tu Estrés",
                        descripcion = "Implementa técnicas de manejo del estrés y busca apoyo emocional.",
                        prioridad = PrioridadRecomendacion.ALTA,
                        categoria = categoria,
                        accionesConcretas = listOf(
                            "Practica mindfulness o meditación 10 min/día",
                            "Establece límites claros entre trabajo y vida personal",
                            "Habla con alguien de confianza sobre cómo te sientes",
                            "Considera terapia psicológica"
                        )
                    ))
                }

                CategoriaFactor.CARGA_TRABAJO -> {
                    recomendaciones.add(Recomendacion(
                        titulo = "Optimiza tu Carga Laboral",
                        descripcion = "Es momento de reorganizar tus tareas y establecer límites.",
                        prioridad = PrioridadRecomendacion.ALTA,
                        categoria = categoria,
                        accionesConcretas = listOf(
                            "Habla con tu supervisor sobre tu carga de trabajo",
                            "Aprende a delegar tareas cuando sea posible",
                            "Prioriza tareas con matriz de Eisenhower",
                            "Di 'no' a compromisos adicionales"
                        )
                    ))
                }

                CategoriaFactor.SUENO -> {
                    recomendaciones.add(Recomendacion(
                        titulo = "Mejora tu Higiene del Sueño",
                        descripcion = "El sueño reparador es esencial para tu recuperación.",
                        prioridad = PrioridadRecomendacion.ALTA,
                        categoria = categoria,
                        accionesConcretas = listOf(
                            "Establece un horario regular para dormir",
                            "Evita pantallas 1 hora antes de dormir",
                            "Crea un ambiente oscuro, fresco y silencioso",
                            "Evita cafeína después de las 2 PM"
                        )
                    ))
                }

                CategoriaFactor.BALANCE_VIDA -> {
                    recomendaciones.add(Recomendacion(
                        titulo = "Recupera tu Balance Vida-Trabajo",
                        descripcion = "Dedica tiempo de calidad a tu vida personal y relaciones.",
                        prioridad = PrioridadRecomendacion.ALTA,
                        categoria = categoria,
                        accionesConcretas = listOf(
                            "Programa actividades placenteras semanalmente",
                            "Dedica tiempo de calidad a familia y amigos",
                            "Desconecta completamente los fines de semana",
                            "Retoma hobbies que habías abandonado"
                        )
                    ))
                }

                CategoriaFactor.ACTIVIDAD_FISICA -> {
                    recomendaciones.add(Recomendacion(
                        titulo = "Incrementa tu Actividad Física",
                        descripcion = "El ejercicio regular reduce estrés y mejora tu ánimo.",
                        prioridad = PrioridadRecomendacion.MEDIA,
                        categoria = categoria,
                        accionesConcretas = listOf(
                            "Camina 30 minutos diarios",
                            "Haz ejercicio moderado 3 veces por semana",
                            "Practica yoga o tai chi para reducir estrés",
                            "Usa las escaleras en lugar del ascensor"
                        )
                    ))
                }

                else -> {}
            }
        }

        return recomendaciones.distinctBy { it.titulo }
            .sortedBy { it.prioridad.ordinal }
    }

    /**
     * Guarda el modelo entrenado
     */
    fun saveModel() {
        val modelFile = File(context.filesDir, MODEL_FILE)
        model?.save(modelFile)
        android.util.Log.d(TAG, "💾 Modelo guardado en ${modelFile.absolutePath}")
    }

    /**
     * Carga un modelo previamente entrenado
     */
    fun loadModel() {
        val modelFile = File(context.filesDir, MODEL_FILE)
        if (modelFile.exists()) {
            model = Sequential.loadModelConfiguration(modelFile)
            isModelTrained = true
            android.util.Log.d(TAG, "📂 Modelo cargado desde ${modelFile.absolutePath}")
        } else {
            android.util.Log.w(TAG, "⚠️ No se encontró modelo guardado")
        }
    }

    /**
     * Verifica si existe un modelo guardado
     */
    fun modelExists(): Boolean {
        return File(context.filesDir, MODEL_FILE).exists()
    }

    /**
     * Libera recursos del modelo
     */
    fun close() {
        model?.close()
        android.util.Log.d(TAG, "🔒 Modelo cerrado y recursos liberados")
    }

    // ============ UTILIDADES ============

    private fun oneHotEncode(label: Int): FloatArray {
        val encoded = FloatArray(OUTPUT_SIZE) { 0f }
        encoded[label] = 1f
        return encoded
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps = exps.sum()
        return exps.map { it / sumExps }.toFloatArray()
    }
}