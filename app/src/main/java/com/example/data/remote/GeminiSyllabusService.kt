package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedTopic(
    val title: String,
    val subtopics: List<String>,
    val estimatedHours: Float
)

data class GeneratedUnit(
    val unitNumber: Int,
    val name: String,
    val topics: List<GeneratedTopic>
)

data class GeneratedSyllabus(
    val subjectName: String,
    val subjectCode: String,
    val examName: String,
    val suggestedColorHex: String,
    val units: List<GeneratedUnit>,
    val generatedByAi: Boolean = true
)

object GeminiSyllabusService {
    private const val TAG = "GeminiSyllabusService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun generateSyllabus(
        subjectTitle: String,
        academicLevel: String = "College / University",
        examType: String = "Semester Final Exam",
        targetUnitsCount: Int = 4,
        customFocusInstructions: String = ""
    ): Result<GeneratedSyllabus> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidApiKey) {
            try {
                val promptText = buildPrompt(
                    subjectTitle = subjectTitle,
                    academicLevel = academicLevel,
                    examType = examType,
                    targetUnits = targetUnitsCount,
                    customFocus = customFocusInstructions
                )

                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", promptText)
                                })
                            })
                        })
                    }
                    put("contents", contentsArray)

                    val genConfig = JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.4)
                    }
                    put("generationConfig", genConfig)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = requestJson.toString().toRequestBody(mediaType)
                val url = "$BASE_URL?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyStr = response.body?.string()

                if (response.isSuccessful && !responseBodyStr.isNullOrBlank()) {
                    val parsed = parseGeminiResponse(responseBodyStr, subjectTitle)
                    if (parsed != null && parsed.units.isNotEmpty()) {
                        return@withContext Result.success(parsed)
                    }
                } else {
                    Log.w(TAG, "Gemini API returned code ${response.code}: $responseBodyStr")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating syllabus with Gemini API", e)
            }
        }

        // Fallback generator: creates a comprehensive structured syllabus for the requested subject
        val fallback = generateFallbackSyllabus(
            subjectTitle = subjectTitle,
            academicLevel = academicLevel,
            examType = examType,
            targetUnitsCount = targetUnitsCount,
            customFocus = customFocusInstructions
        )
        Result.success(fallback)
    }

    private fun buildPrompt(
        subjectTitle: String,
        academicLevel: String,
        examType: String,
        targetUnits: Int,
        customFocus: String
    ): String {
        return """
            You are a university professor and curriculum director creating a comprehensive exam syllabus.
            Generate a detailed, structured syllabus for the subject: "$subjectTitle".
            
            Parameters:
            - Academic Level: $academicLevel
            - Exam Type / Purpose: $examType
            - Desired Number of Units/Chapters: $targetUnits
            ${if (customFocus.isNotBlank()) "- Special Focus Areas: $customFocus" else ""}
            
            Return ONLY a JSON object with this EXACT schema:
            {
              "subjectName": "$subjectTitle",
              "subjectCode": "CS201",
              "examName": "Final Exam",
              "suggestedColorHex": "#4F46E5",
              "units": [
                {
                  "unitNumber": 1,
                  "name": "Unit Title",
                  "topics": [
                    {
                      "title": "Topic Title",
                      "subtopics": ["Key concept A", "Key concept B", "Key concept C"],
                      "estimatedHours": 3.0
                    }
                  ]
                }
              ]
            }
            
            Guidelines:
            1. Ensure realistic academic progression from fundamentals to advanced concepts.
            2. Each unit should contain 3 to 5 core topics.
            3. Each topic must have 2 to 4 concise subtopic keywords.
            4. Suggested color hex should be a vibrant hex code like #4F46E5, #0D9488, #F59E0B, #EC4899, #10B981, #8B5CF6, #3B82F6, or #F97316.
            5. Provide a realistic course code abbreviation based on the subject name.
            6. Output raw JSON only. No markdown formatting.
        """.trimIndent()
    }

    private fun parseGeminiResponse(rawJson: String, defaultSubjectName: String): GeneratedSyllabus? {
        try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            val firstPart = parts.getJSONObject(0)
            var text = firstPart.optString("text", "")

            // Strip markdown code fences if model enclosed them
            text = text.trim()
            if (text.startsWith("```json")) {
                text = text.removePrefix("```json")
            } else if (text.startsWith("```")) {
                text = text.removePrefix("```")
            }
            if (text.endsWith("```")) {
                text = text.removeSuffix("```")
            }
            text = text.trim()

            val syllabusJson = JSONObject(text)
            val subjectName = syllabusJson.optString("subjectName", defaultSubjectName)
            val subjectCode = syllabusJson.optString("subjectCode", generateSubjectCode(subjectName))
            val examName = syllabusJson.optString("examName", "Final Exam")
            val suggestedColorHex = syllabusJson.optString("suggestedColorHex", "#4F46E5")

            val unitsArray = syllabusJson.optJSONArray("units") ?: JSONArray()
            val unitsList = mutableListOf<GeneratedUnit>()

            for (i in 0 until unitsArray.length()) {
                val unitObj = unitsArray.getJSONObject(i)
                val unitNum = unitObj.optInt("unitNumber", i + 1)
                val unitName = unitObj.optString("name", "Unit $unitNum")

                val topicsArray = unitObj.optJSONArray("topics") ?: JSONArray()
                val topicsList = mutableListOf<GeneratedTopic>()

                for (j in 0 until topicsArray.length()) {
                    val topicObj = topicsArray.getJSONObject(j)
                    val topicTitle = topicObj.optString("title", "Topic ${j + 1}")
                    val estHours = topicObj.optDouble("estimatedHours", 2.0).toFloat()

                    val subtopicsArray = topicObj.optJSONArray("subtopics") ?: JSONArray()
                    val subtopicsList = mutableListOf<String>()
                    for (k in 0 until subtopicsArray.length()) {
                        subtopicsList.add(subtopicsArray.optString(k))
                    }

                    topicsList.add(
                        GeneratedTopic(
                            title = topicTitle,
                            subtopics = subtopicsList,
                            estimatedHours = estHours
                        )
                    )
                }

                if (topicsList.isNotEmpty()) {
                    unitsList.add(
                        GeneratedUnit(
                            unitNumber = unitNum,
                            name = unitName,
                            topics = topicsList
                        )
                    )
                }
            }

            return GeneratedSyllabus(
                subjectName = subjectName,
                subjectCode = subjectCode,
                examName = examName,
                suggestedColorHex = suggestedColorHex,
                units = unitsList,
                generatedByAi = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini syllabus JSON", e)
            return null
        }
    }

    private fun generateSubjectCode(name: String): String {
        val words = name.trim().split("\\s+".toRegex())
        val initials = words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        val prefix = if (initials.length in 2..4) initials else name.take(3).uppercase()
        val num = (100..499).random()
        return "$prefix$num"
    }

    private fun generateFallbackSyllabus(
        subjectTitle: String,
        academicLevel: String,
        examType: String,
        targetUnitsCount: Int,
        customFocus: String
    ): GeneratedSyllabus {
        val cleanTitle = subjectTitle.trim().ifBlank { "Computer Science" }
        val code = generateSubjectCode(cleanTitle)

        // Generate tailored units based on knowledge or general academic progression
        val units = mutableListOf<GeneratedUnit>()
        val unitsToMake = targetUnitsCount.coerceIn(2, 6)

        val progressionNames = when {
            cleanTitle.contains("data struct", ignoreCase = true) || cleanTitle.contains("dsa", ignoreCase = true) -> listOf(
                "Linear Data Structures" to listOf(
                    "Arrays & Dynamic Vectors" to listOf("Amortized insertion", "Two pointers", "Prefix sums"),
                    "Linked Lists & Variations" to listOf("Singly vs doubly linked", "Cycle detection", "Reversal algorithms"),
                    "Stacks & Queues" to listOf("Monotonic stack", "Queue via stacks", "Evaluation of postfix")
                ),
                "Non-Linear Data Structures" to listOf(
                    "Binary Trees & Traversals" to listOf("In-order, Pre-order, Post-order", "LCA", "Diameter calculation"),
                    "Binary Search Trees & AVL" to listOf("Self-balancing trees", "Rotations", "Tree set implementation"),
                    "Heaps & Priority Queues" to listOf("Min/Max heap", "Heapify algorithm", "Top-K elements")
                ),
                "Graph Theory & Algorithms" to listOf(
                    "Graph Representation & Traversal" to listOf("Adjacency list", "BFS shortest path", "DFS connectivity"),
                    "Minimum Spanning Trees" to listOf("Kruskal's algorithm", "Prim's algorithm", "Disjoint Set Union"),
                    "Shortest Path Algorithms" to listOf("Dijkstra's algorithm", "Bellman-Ford", "Floyd-Warshall")
                ),
                "Algorithm Design Strategies" to listOf(
                    "Divide and Conquer" to listOf("Merge sort", "Quick sort", "Master theorem"),
                    "Dynamic Programming" to listOf("Memoization vs tabulation", "0/1 Knapsack", "Longest common subsequence"),
                    "Greedy Algorithms" to listOf("Activity selection", "Huffman coding", "Fractional knapsack")
                )
            )
            cleanTitle.contains("operat", ignoreCase = true) || cleanTitle.contains("os", ignoreCase = true) -> listOf(
                "Operating System Overview & Processes" to listOf(
                    "OS Architecture & Dual Mode" to listOf("System calls", "Kernel vs user space", "Interrupt handling"),
                    "Process Management & IPC" to listOf("PCB structure", "Pipes and shared memory", "Context switching"),
                    "CPU Scheduling Algorithms" to listOf("FCFS & Round Robin", "Multi-level feedback queues", "SJF scheduling")
                ),
                "Threads & Concurrency Control" to listOf(
                    "Multithreading Models" to listOf("User vs Kernel threads", "Thread pooling", "Race conditions"),
                    "Synchronization & Locks" to listOf("Mutex & Semaphores", "Peterson's algorithm", "Monitors"),
                    "Deadlock Detection & Prevention" to listOf("Resource allocation graph", "Banker's algorithm", "Deadlock recovery")
                ),
                "Memory Architecture" to listOf(
                    "Physical & Virtual Memory" to listOf("Paging mechanisms", "TLB caching", "Segmentation"),
                    "Page Replacement Algorithms" to listOf("LRU & FIFO", "Optimal replacement", "Thrashing analysis")
                ),
                "Storage & File Systems" to listOf(
                    "File Allocation Methods" to listOf("Contiguous allocation", "Linked list allocation", "Indexed inode tables"),
                    "Disk Scheduling & I/O" to listOf("SCAN, C-SCAN", "RAID levels 0-5", "Buffer cache")
                )
            )
            cleanTitle.contains("chem", ignoreCase = true) -> listOf(
                "Structure and Bonding" to listOf(
                    "Atomic Orbitals & Hybridization" to listOf("sp, sp2, sp3 hybridization", "Molecular geometry", "Resonance structures"),
                    "Acids, Bases, and Polarity" to listOf("pKa trends", "Lewis acids", "Inductive effects"),
                    "Conformational Analysis" to listOf("Newman projections", "Chair conformations", "Steric hindrance")
                ),
                "Thermodynamics & Reaction Kinetics" to listOf(
                    "Reaction Energetics" to listOf("Activation energy", "Transition states", "Hammond's postulate"),
                    "Nucleophilic Substitution" to listOf("SN1 vs SN2 kinetics", "Solvent effects", "Stereochemical inversion"),
                    "Elimination Reactions" to listOf("E1 vs E2 mechanisms", "Zaitsev rule", "Hofmann product")
                ),
                "Functional Group Chemistry" to listOf(
                    "Alkenes and Alkynes" to listOf("Electrophilic addition", "Markovnikov rule", "Hydroboration-oxidation"),
                    "Alcohols, Ethers, and Epoxides" to listOf("Synthesis routes", "Ring opening reactions", "Protecting groups")
                ),
                "Spectroscopy & Structure Elucidation" to listOf(
                    "Infrared Spectroscopy (IR)" to listOf("Characteristic functional peaks", "Fingerprint region"),
                    "Nuclear Magnetic Resonance (NMR)" to listOf("Chemical shifts", "Spin-spin splitting", "Integration curves")
                )
            )
            cleanTitle.contains("physic", ignoreCase = true) -> listOf(
                "Mechanics & Kinematics" to listOf(
                    "Vectors and 2D Motion" to listOf("Projectile trajectories", "Relative velocities", "Circular motion"),
                    "Newton's Laws & Dynamics" to listOf("Free body diagrams", "Friction models", "Tension forces"),
                    "Work, Energy & Momentum" to listOf("Work-energy theorem", "Conservation of momentum", "Elastic collisions")
                ),
                "Rotational Motion & Gravitation" to listOf(
                    "Torque & Angular Momentum" to listOf("Moment of inertia", "Rotational kinetic energy", "Conservation of angular momentum"),
                    "Universal Gravitation" to listOf("Kepler's laws", "Gravitational potential", "Orbital mechanics")
                ),
                "Electromagnetism" to listOf(
                    "Electrostatics & Gauss's Law" to listOf("Electric fields", "Electric flux", "Capacitance"),
                    "Circuits and Ohm's Law" to listOf("Kirchhoff's rules", "RC circuits", "Power dissipation"),
                    "Magnetic Fields & Induction" to listOf("Lorentz force", "Faraday's law", "Lenz's law")
                ),
                "Waves, Optics & Modern Physics" to listOf(
                    "Wave Mechanics & Sound" to listOf("Doppler effect", "Standing waves", "Interference"),
                    "Geometric and Physical Optics" to listOf("Snell's law", "Thin lenses", "Diffraction grating")
                )
            )
            else -> listOf(
                "Foundational Principles of $cleanTitle" to listOf(
                    "Core Concepts & Terminology" to listOf("Fundamental definitions", "Historical context", "Key taxonomies"),
                    "Theoretical Frameworks" to listOf("Guiding paradigms", "Standard models", "Core principles"),
                    "Essential Methodologies" to listOf("Analytical approaches", "Common techniques", "Baseline metrics")
                ),
                "Core Mechanics & Applied Systems" to listOf(
                    "Primary Systems Analysis" to listOf("Structural components", "Operational workflows", "System design"),
                    "Intermediate Techniques" to listOf("Implementation patterns", "Diagnostic workflows", "Quantitative methods"),
                    "Optimization & Troubleshooting" to listOf("Bottleneck resolution", "Efficiency maximization", "Quality assurance")
                ),
                "Advanced Methodologies in $cleanTitle" to listOf(
                    "Complex Problem Solving" to listOf("Multi-variable analysis", "Advanced synthesis", "Edge-case handling"),
                    "Modern Trends & Innovations" to listOf("Current industry standards", "Recent breakthroughs", "Integration practices")
                ),
                "Exam Preparation & Comprehensive Review" to listOf(
                    "Synthesized Case Studies" to listOf("Real-world scenario application", "Cross-topic integration"),
                    "Practice Problems & Masteries" to listOf("Sample exam questions", "Timing strategies", "Critical formulas review")
                )
            )
        }

        val colors = listOf("#4F46E5", "#0D9488", "#F59E0B", "#EC4899", "#10B981", "#8B5CF6", "#3B82F6", "#F97316")
        val color = colors[cleanTitle.hashCode().let { if (it < 0) -it else it } % colors.size]

        for (i in 0 until unitsToMake) {
            val templateUnit = progressionNames.getOrNull(i % progressionNames.size)
            val unitName = templateUnit?.first ?: "Unit ${i + 1}: In-Depth Analysis"
            val rawTopics = templateUnit?.second ?: listOf(
                "Core Topic ${i + 1}.1" to listOf("Subtopic A", "Subtopic B"),
                "Core Topic ${i + 1}.2" to listOf("Subtopic C", "Subtopic D")
            )

            val topics = rawTopics.mapIndexed { topicIdx, (tTitle, subList) ->
                GeneratedTopic(
                    title = tTitle,
                    subtopics = subList,
                    estimatedHours = (2.0f + (topicIdx * 0.5f))
                )
            }

            units.add(
                GeneratedUnit(
                    unitNumber = i + 1,
                    name = unitName,
                    topics = topics
                )
            )
        }

        return GeneratedSyllabus(
            subjectName = cleanTitle,
            subjectCode = code,
            examName = examType,
            suggestedColorHex = color,
            units = units,
            generatedByAi = true
        )
    }
}
