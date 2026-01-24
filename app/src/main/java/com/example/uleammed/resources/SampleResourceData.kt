package com.example.uleammed.resources

import java.util.concurrent.TimeUnit

/**
 * Datos de muestra para recursos de salud laboral
 * Contenido educativo real y científicamente validado
 */
object SampleResourceData {

    fun getSampleResources(): List<ResourceItem> {
        return listOf(
            // ============ ERGONOMÍA ============
            ResourceItem(
                id = "erg_001",
                type = ResourceType.ARTICLE,
                category = ResourceCategory.ERGONOMICS,
                title = "Configura tu Estación de Trabajo Perfecta",
                summary = "Guía completa para organizar tu espacio de trabajo y prevenir lesiones músculo-esqueléticas.",
                content = """
# Configura tu Estación de Trabajo Perfecta

## La Regla 90-90-90

La postura ergonómica ideal sigue la regla 90-90-90:
- **90° en las rodillas**: Los pies deben estar planos en el suelo
- **90° en las caderas**: Muslos paralelos al suelo
- **90° en los codos**: Brazos formando ángulo recto

## Altura del Monitor

El borde superior de tu monitor debe estar a la altura de tus ojos o ligeramente por debajo (hasta 15 cm). Esto previene:
- Tensión cervical
- Fatiga ocular
- Dolores de cabeza

## Distancia del Monitor

Mantén una distancia de **50-70 cm** (longitud de un brazo) entre tus ojos y la pantalla.

## Posición del Teclado

El teclado debe estar:
- Directamente frente a ti
- A la altura de los codos
- Con inclinación negativa o plana

## Iluminación

- Luz natural indirecta es ideal
- Evita reflejos en la pantalla
- Usa lámpara de escritorio adicional si es necesario

## Pausas Activas

Levántate y muévete cada 30-60 minutos. Esto mejora la circulación y reduce la fatiga.

---

**Fuente**: OSHA (Occupational Safety and Health Administration)
                """.trimIndent(),
                source = "OSHA",
                sourceUrl = "https://www.osha.gov/ergonomics",
                readTime = "5 min",
                difficulty = ResourceDifficulty.BASIC,
                tags = listOf("ergonomía", "prevención", "postura", "escritorio"),
                isPeerReviewed = true,
                keyPoints = listOf(
                    "Regla 90-90-90 para postura óptima",
                    "Monitor a la altura de los ojos",
                    "Pausas cada 30-60 minutos",
                    "Iluminación indirecta sin reflejos"
                ),
                references = listOf(
                    "OSHA Ergonomics Guidelines 2023",
                    "NIOSH - Computer Workstation Setup"
                ),
                publishDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
            ),

            ResourceItem(
                id = "erg_002",
                type = ResourceType.GUIDE,
                category = ResourceCategory.ERGONOMICS,
                title = "Checklist de Evaluación Ergonómica",
                summary = "Lista de verificación completa para auditar tu espacio de trabajo.",
                content = """
# Checklist de Evaluación Ergonómica

## 📋 Silla

- [ ] Tiene ajuste de altura
- [ ] Tiene soporte lumbar ajustable
- [ ] Los pies llegan al suelo
- [ ] Tiene apoyabrazos ajustables
- [ ] El respaldo reclinable funciona

## 🖥️ Monitor

- [ ] Borde superior a la altura de los ojos
- [ ] Distancia de 50-70 cm
- [ ] Sin reflejos ni brillos
- [ ] Brillo ajustado a la iluminación ambiente
- [ ] Pantalla perpendicular a ventanas

## ⌨️ Teclado y Mouse

- [ ] A la altura de los codos
- [ ] Muñecas rectas al escribir
- [ ] Mouse cerca del teclado
- [ ] Almohadilla de soporte disponible

## 💡 Iluminación

- [ ] Luz general suficiente (500 lux)
- [ ] Sin reflejos en pantalla
- [ ] Lámpara de escritorio si es necesario
- [ ] Contraste adecuado

## 🪑 Espacio

- [ ] Suficiente espacio para piernas
- [ ] Escritorio a 68-76 cm de altura
- [ ] Todo al alcance sin estirarse
- [ ] Cables organizados

## 🧘 Hábitos

- [ ] Pausas cada 30-60 minutos
- [ ] Estiramientos regulares
- [ ] Variedad de posturas
- [ ] Hidratación constante

---

**Puntuación**:
- 18-20 ✅ Excelente
- 14-17 ⚠️ Mejorable  
- <14 ❌ Requiere atención inmediata
                """.trimIndent(),
                source = "Cornell University Ergonomics",
                sourceUrl = "https://ergo.human.cornell.edu",
                readTime = "3 min",
                difficulty = ResourceDifficulty.BASIC,
                tags = listOf("checklist", "evaluación", "audit"),
                pdfUrl = "https://example.com/checklist.pdf",
                keyPoints = listOf(
                    "20 puntos de verificación esenciales",
                    "Sistema de puntuación simple",
                    "Acción inmediata según resultado"
                )
            ),

            // ============ SÍNTOMAS MÚSCULO-ESQUELÉTICOS ============
            ResourceItem(
                id = "msk_001",
                type = ResourceType.ARTICLE,
                category = ResourceCategory.MUSCULOSKELETAL,
                title = "Síndrome del Túnel Carpiano: Prevención y Tratamiento",
                summary = "Comprende qué es, cómo prevenirlo y tratarlo efectivamente.",
                content = """
# Síndrome del Túnel Carpiano

## ¿Qué es?

El túnel carpiano es un estrecho pasaje en la muñeca por donde pasa el nervio mediano. Cuando este se comprime, aparecen síntomas como:

- Hormigueo y entumecimiento en dedos (pulgar, índice, medio)
- Dolor en muñeca que sube por el brazo
- Debilidad en la mano
- Dificultad para agarrar objetos

## Causas Laborales

- Movimientos repetitivos de muñeca
- Uso prolongado de teclado/mouse
- Postura inadecuada de manos
- Presión en la base de la palma

## Prevención

### 1. Postura Correcta
- Muñecas rectas al escribir
- Codos a 90°
- Antebrazos paralelos al suelo

### 2. Pausas Frecuentes
- Cada 30 minutos
- Estirar manos y muñecas
- Sacudir manos suavemente

### 3. Equipamiento
- Teclado ergonómico
- Mouse vertical
- Almohadillas de soporte

## Ejercicios Preventivos

### Estiramiento de Muñeca
1. Extiende el brazo frente a ti
2. Con la otra mano, tira dedos hacia atrás
3. Mantén 15 segundos
4. Repite 3 veces cada mano

### Rotación de Muñeca
1. Cierra el puño
2. Rota la muñeca en círculos
3. 10 repeticiones en cada dirección

## ¿Cuándo Consultar al Médico?

- Síntomas que despiertan por la noche
- Duración mayor a 2 semanas
- Pérdida de fuerza en la mano
- Dificultad en actividades diarias

## Tratamiento

El tratamiento temprano es crucial:
- Férulas nocturnas
- Fisioterapia
- Modificaciones ergonómicas
- En casos severos: cirugía

---

**Fuente**: Mayo Clinic, American Academy of Orthopaedic Surgeons
                """.trimIndent(),
                source = "Mayo Clinic",
                sourceUrl = "https://www.mayoclinic.org",
                readTime = "7 min",
                difficulty = ResourceDifficulty.INTERMEDIATE,
                tags = listOf("túnel carpiano", "prevención", "muñeca", "dolor"),
                isPeerReviewed = true,
                keyPoints = listOf(
                    "Hormigueo nocturno es señal de alerta",
                    "Prevención a través de ergonomía",
                    "Ejercicios diarios efectivos",
                    "Tratamiento temprano previene cirugía"
                ),
                references = listOf(
                    "Mayo Clinic - Carpal Tunnel Syndrome (2023)",
                    "AAOS Clinical Practice Guidelines"
                )
            ),

            // ============ SALUD VISUAL ============
            ResourceItem(
                id = "vis_001",
                type = ResourceType.ARTICLE,
                category = ResourceCategory.VISUAL,
                title = "La Regla 20-20-20 para Prevenir Fatiga Visual",
                summary = "Técnica científicamente probada para proteger tus ojos frente a pantallas.",
                content = """
# La Regla 20-20-20

## ¿Qué es?

Cada **20 minutos** de trabajo frente a pantalla:
1. Mira a **20 pies** de distancia (6 metros)
2. Durante **20 segundos**
3. Parpadea conscientemente

## Base Científica

Estudios demuestran que esta técnica:
- Reduce fatiga ocular en 50%
- Previene sequedad ocular
- Disminuye dolores de cabeza
- Mejora enfoque y productividad

## ¿Por qué Funciona?

### Relajación Muscular
Mirar a lo lejos relaja los músculos ciliares del ojo, responsables del enfoque cercano.

### Parpadeo
Frente a pantallas parpadeamos 66% menos, causando sequedad. La regla nos recuerda parpadear.

## Implementación Práctica

### Alarmas
- Configura recordatorios cada 20 minutos
- Apps recomendadas: Time Out, Eye Care 20-20-20

### Objetos de Referencia
Identifica un objeto a 6 metros (ventana, cuadro) como referencia visual.

## Complementos

### Filtros de Luz Azul
- Lentes con filtro
- Modo nocturno en dispositivos
- Apps como f.lux

### Humedad
- Parpadeo consciente
- Gotas lubricantes
- Humidificador en oficina

### Iluminación
- Evita reflejos en pantalla
- Iluminación ambiental similar a pantalla
- Luz natural indirecta

## Síntomas de Fatiga Visual

- Visión borrosa
- Ojos secos o llorosos
- Dolor de cabeza
- Sensibilidad a la luz
- Dificultad para enfocar

Si persisten, consulta un oftalmólogo.

---

**Fuente**: American Optometric Association
                """.trimIndent(),
                source = "American Optometric Association",
                sourceUrl = "https://www.aoa.org",
                readTime = "4 min",
                difficulty = ResourceDifficulty.BASIC,
                tags = listOf("fatiga visual", "20-20-20", "pantallas", "ojos"),
                isPeerReviewed = true,
                videoUrl = "dQw4w9WgXcQ", // ID de YouTube de ejemplo
                keyPoints = listOf(
                    "Cada 20 min, mira 20 pies, por 20 seg",
                    "Reduce fatiga visual 50%",
                    "Complementar con filtros de luz azul",
                    "Parpadeo consciente esencial"
                )
            ),

            // ============ SALUD MENTAL ============
            ResourceItem(
                id = "mh_001",
                type = ResourceType.ARTICLE,
                category = ResourceCategory.MENTAL_HEALTH,
                title = "Diferencia entre Estrés y Burnout",
                summary = "Aprende a identificar y actuar ante cada condición.",
                content = """
# Estrés vs Burnout

## Estrés Laboral

### Definición
Respuesta emocional y física a demandas excesivas o prolongadas.

### Características
- **Temporal**: Relacionado con situación específica
- **Hiperactivación**: Sensación de urgencia constante
- **Emocional**: Ansiedad, irritabilidad
- **Reversible**: Con descanso y cambios

### Síntomas
- Corazón acelerado
- Dificultad para concentrarse
- Problemas de sueño
- Tensión muscular

## Burnout (Síndrome de Desgaste)

### Definición
Estado de agotamiento físico, emocional y mental debido a estrés crónico no gestionado.

### Características
- **Crónico**: Desarrollado en meses/años
- **Desconexión**: Cinismo y despersonalización
- **Agotamiento**: Vacío emocional
- **Requiere intervención**: No se resuelve solo

### Dimensiones del Burnout

#### 1. Agotamiento Emocional
- "No tengo nada más que dar"
- Fatiga profunda constante
- Incapacidad de recuperarse con descanso

#### 2. Despersonalización
- Distanciamiento de estudiantes/colegas
- Cinismo hacia el trabajo
- Pérdida de empatía

#### 3. Baja Realización Personal
- Sensación de ineficacia
- Dudas sobre competencia
- Pérdida de logros

## Tabla Comparativa

| Aspecto | Estrés | Burnout |
|---------|--------|---------|
| Duración | Episódico | Crónico |
| Energía | Hiperactividad | Agotamiento |
| Emoción | Ansiedad | Vacío |
| Motivación | Disminuida | Perdida |
| Esperanza | Mejorará | Sin esperanza |
| Recuperación | Descanso | Intervención |

## ¿Qué Hacer?

### Ante Estrés
- Pausas regulares
- Técnicas de relajación
- Ejercicio físico
- Organización y priorización
- Apoyo social

### Ante Burnout
- **Consulta profesional obligatoria**
- Posible baja médica
- Reevaluación laboral profunda
- Terapia psicológica
- Cambios significativos

## Señales de Alarma

Busca ayuda si:
- Síntomas interfieren con vida diaria
- Pensamiento de que "no vale la pena"
- Aislamiento social
- Cambios drásticos de humor
- Pensamientos de autolesión

---

**Importante**: Este contenido es educativo. Si experimentas síntomas severos, consulta un profesional de salud mental.

**Fuente**: OMS, ICD-11 (Burnout como fenómeno ocupacional)
                """.trimIndent(),
                source = "OMS",
                readTime = "8 min",
                difficulty = ResourceDifficulty.INTERMEDIATE,
                tags = listOf("burnout", "estrés", "salud mental", "agotamiento"),
                isPeerReviewed = true,
                keyPoints = listOf(
                    "Estrés es temporal, burnout es crónico",
                    "Burnout tiene 3 dimensiones: agotamiento, despersonalización, ineficacia",
                    "Burnout requiere intervención profesional",
                    "Identificación temprana previene cronificación"
                ),
                references = listOf(
                    "ICD-11 - Burn-out classification (WHO, 2022)",
                    "Maslach Burnout Inventory"
                )
            ),

            // ============ SUEÑO ============
            ResourceItem(
                id = "slp_001",
                type = ResourceType.GUIDE,
                category = ResourceCategory.SLEEP,
                title = "Higiene del Sueño para Trabajadores Digitales",
                summary = "Mejora la calidad de tu descanso con hábitos respaldados científicamente.",
                content = """
# Higiene del Sueño

## ¿Qué es?

Conjunto de prácticas y hábitos para optimizar la calidad del sueño.

## Reglas de Oro

### 1. Horario Consistente
- Acuéstate y levántate a la misma hora
- Incluso fines de semana (±1 hora máximo)
- El cerebro ama la rutina

### 2. Ambiente Ideal

#### Temperatura
- **18-20°C** (64-68°F)
- Más fresco = mejor sueño

#### Oscuridad
- Cortinas opacas
- Sin luces LED (cubrir dispositivos)
- Antifaz si es necesario

#### Silencio
- Tapones si hay ruido
- Ruido blanco/marrón como alternativa

### 3. Desconexión Digital

#### 2 Horas Antes
- Apaga pantallas brillantes
- Activa modo nocturno
- Luz cálida en casa

#### Por qué
La luz azul suprime melatonina (hormona del sueño) hasta 3 horas.

### 4. Rutina Pre-Sueño

Crea secuencia relajante de 30-60 min:
- Lectura (libro físico)
- Ducha tibia
- Estiramientos suaves
- Meditación/respiración

### 5. La Cama es SOLO para Dormir

- No trabajo
- No TV
- No móvil
- Sexo o dormir únicamente

Tu cerebro asociará cama = sueño.

### 6. Alimentación

#### ✅ Permitido
- Cena ligera 2-3h antes
- Infusiones sin cafeína (manzanilla, tilo)
- Snack pequeño si tienes hambre

#### ❌ Evitar
- Cafeína después de las 14:00
- Alcohol (fragmenta el sueño)
- Comidas pesadas o picantes
- Líquidos excesivos (despertares nocturnos)

### 7. Ejercicio

- Regular pero NO cerca de dormir
- Ideal: mañana o tarde
- Mínimo 3-4 horas antes de acostarse

### 8. Técnica 4-7-8

Si cuesta conciliar:
1. Inhala 4 segundos
2. Aguanta 7 segundos
3. Exhala 8 segundos
4. Repite 4 veces

Activa sistema nervioso parasimpático.

## Errores Comunes

### Siesta Excesiva
- Máximo 20-30 minutos
- Antes de las 15:00
- Más tiempo/tarde interfiere con sueño nocturno

### "Recuperar" Sueño en Fin de Semana
- Desajusta ritmo circadiano
- Lunes más difícil ("jet lag social")

### Mirar el Reloj
- Genera ansiedad
- Aleja el móvil
- Reloj fuera de vista

## ¿Cuánto Necesitas?

| Edad | Horas |
|------|-------|
| 18-25 | 7-9 |
| 26-64 | 7-9 |
| 65+ | 7-8 |

La calidad importa tanto como la cantidad.

## Señales de Problema Serio

Consulta médico si:
- Ronquidos fuertes (apnea del sueño)
- Insomnio >3 semanas
- Somnolencia diurna extrema
- Piernas inquietas nocturnas

---

**Fuente**: National Sleep Foundation, American Academy of Sleep Medicine
                """.trimIndent(),
                source = "National Sleep Foundation",
                readTime = "6 min",
                difficulty = ResourceDifficulty.BASIC,
                tags = listOf("sueño", "insomnio", "descanso", "melatonina"),
                isPeerReviewed = true,
                keyPoints = listOf(
                    "Horario consistente es fundamental",
                    "Desconexión digital 2h antes",
                    "Ambiente: 18-20°C, oscuro, silencioso",
                    "Cama solo para dormir"
                )
            )
        )
    }

    fun getSampleExercises(): List<ExerciseResource> {
        return listOf(
            ExerciseResource(
                id = "ex_001",
                name = "Estiramiento de Cuello",
                description = "Alivia tensión cervical y previene dolores de cabeza.",
                category = ResourceCategory.MUSCULOSKELETAL,
                duration = 60,
                repetitions = 3,
                sets = 1,
                instructions = listOf(
                    "Siéntate con espalda recta",
                    "Inclina la cabeza hacia la derecha, llevando oreja al hombro",
                    "Mantén 20 segundos sin forzar",
                    "Vuelve al centro",
                    "Repite hacia el lado izquierdo"
                ),
                benefits = listOf(
                    "Reduce tensión cervical",
                    "Previene dolores de cabeza",
                    "Mejora movilidad del cuello"
                ),
                warnings = listOf(
                    "No forzar el estiramiento",
                    "Si hay dolor agudo, detener"
                ),
                difficulty = ResourceDifficulty.BASIC
            ),

            ExerciseResource(
                id = "ex_002",
                name = "Ejercicio Ocular 20-20-20",
                description = "Previene fatiga visual digital.",
                category = ResourceCategory.VISUAL,
                duration = 20,
                repetitions = 1,
                sets = 1,
                instructions = listOf(
                    "Aparta la mirada de la pantalla",
                    "Mira un objeto a 6 metros (20 pies)",
                    "Mantén la mirada 20 segundos",
                    "Parpadea conscientemente varias veces",
                    "Vuelve al trabajo"
                ),
                benefits = listOf(
                    "Reduce fatiga ocular",
                    "Previene sequedad",
                    "Relaja músculos oculares"
                ),
                difficulty = ResourceDifficulty.BASIC
            ),

            ExerciseResource(
                id = "ex_003",
                name = "Respiración 4-7-8",
                description = "Técnica de relajación para reducir estrés y ansiedad.",
                category = ResourceCategory.MENTAL_HEALTH,
                duration = 60,
                repetitions = 4,
                sets = 1,
                instructions = listOf(
                    "Siéntate cómodamente con espalda recta",
                    "Coloca la lengua detrás de los dientes superiores",
                    "Exhala completamente por la boca",
                    "Inhala por la nariz contando hasta 4",
                    "Aguanta la respiración contando hasta 7",
                    "Exhala por la boca contando hasta 8",
                    "Repite 4 ciclos completos"
                ),
                benefits = listOf(
                    "Reduce ansiedad instantáneamente",
                    "Activa sistema nervioso parasimpático",
                    "Mejora calidad del sueño",
                    "Reduce presión arterial"
                ),
                warnings = listOf(
                    "Puede causar mareo al inicio (normal)",
                    "No exceder 4 ciclos al principio"
                ),
                difficulty = ResourceDifficulty.BASIC
            )
        )
    }

    fun getSampleFAQs(): List<FAQItem> {
        return listOf(
            FAQItem(
                id = "faq_001",
                question = "¿Por qué me duele más el cuello al final del día?",
                answer = """
La tensión cervical acumulativa es común en trabajadores de oficina. Durante el día:

1. **Postura estática**: Mantener el cuello en la misma posición durante horas causa fatiga muscular.

2. **Posición adelantada**: Por cada pulgada que la cabeza se adelanta, añades 10 libras de presión al cuello.

3. **Tensión acumulada**: Los músculos del cuello trabajan constantemente para sostener la cabeza (4-5 kg).

**Solución**: Ajusta la altura del monitor, haz pausas cada 30 minutos, y practica estiramientos cervicales.
                """.trimIndent(),
                category = ResourceCategory.MUSCULOSKELETAL,
                sources = listOf(
                    "Journal of Physical Therapy Science (2017)",
                    "NIOSH - Computer Workstation Ergonomics"
                )
            ),

            FAQItem(
                id = "faq_002",
                question = "¿Es normal sentir hormigueo en las manos al despertar?",
                answer = """
El hormigueo nocturno en manos puede indicar:

**Síndrome del Túnel Carpiano**: Compresión del nervio mediano en la muñeca. Síntomas:
- Hormigueo en pulgar, índice, medio
- Empeora por la noche
- Mejora al sacudir las manos

**Causas**: Posición de muñeca al dormir + movimientos repetitivos diurnos.

**Acción**: Si es frecuente (>3 veces/semana durante 2 semanas), consulta un médico. Puede requerir férula nocturna.
                """.trimIndent(),
                category = ResourceCategory.MUSCULOSKELETAL,
                sources = listOf(
                    "Mayo Clinic - Carpal Tunnel Syndrome",
                    "American Academy of Orthopaedic Surgeons"
                )
            ),

            FAQItem(
                id = "faq_003",
                question = "¿Cuántas horas debo dormir si trabajo frente a pantalla?",
                answer = """
**Respuesta corta**: 7-9 horas por noche.

**Por qué es importante**:

Los trabajadores digitales necesitan **al menos tanto** como cualquier persona, y posiblemente más por:

1. **Fatiga cognitiva**: Trabajo mental intenso
2. **Tensión ocular**: Cansancio visual adicional
3. **Estrés tecnológico**: Multitarea digital
4. **Luz azul**: Puede retrasar el sueño natural

**Calidad sobre cantidad**: 7 horas de sueño profundo y continuo son mejores que 9 horas fragmentadas.

**Tip**: Desconecta pantallas 2 horas antes de dormir para optimizar la producción de melatonina.
                """.trimIndent(),
                category = ResourceCategory.SLEEP,
                sources = listOf(
                    "National Sleep Foundation",
                    "American Academy of Sleep Medicine"
                )
            )
        )
    }
}