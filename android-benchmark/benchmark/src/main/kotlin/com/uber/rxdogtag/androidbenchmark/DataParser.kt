package com.uber.rxdogtag.androidbenchmark

import java.util.Locale

fun main() {

  val data = """
benchmark:        14,706 ns RxDogTagAndroidPerf.observable_complex[enabled=true,times=0]
benchmark:        27,480 ns RxDogTagAndroidPerf.flowable_complex[enabled=true,times=0]
benchmark:        65,251 ns RxDogTagAndroidPerf.observable_e2e[enabled=true,times=0]
benchmark:         8,921 ns RxDogTagAndroidPerf.flowable_simple[enabled=true,times=0]
benchmark:       141,250 ns RxDogTagAndroidPerf.flowable_e2e[enabled=true,times=0]
benchmark:        17,119 ns RxDogTagAndroidPerf.observable_simple[enabled=true,times=0]
benchmark:        24,133 ns RxDogTagAndroidPerf.observable_complex[enabled=true,times=1]
Timed out waiting for process to appear on google-pixel_3-89UX0H5NB.
benchmark:        49,648 ns RxDogTagAndroidPerf.flowable_complex[enabled=true,times=1]
benchmark:       116,597 ns RxDogTagAndroidPerf.observable_e2e[enabled=true,times=1]
benchmark:        23,003 ns RxDogTagAndroidPerf.flowable_simple[enabled=true,times=1]
benchmark:       237,361 ns RxDogTagAndroidPerf.flowable_e2e[enabled=true,times=1]
benchmark:        22,846 ns RxDogTagAndroidPerf.observable_simple[enabled=true,times=1]
benchmark:        19,945 ns RxDogTagAndroidPerf.observable_complex[enabled=true,times=1,000]
benchmark:        51,131 ns RxDogTagAndroidPerf.flowable_complex[enabled=true,times=1,000]
benchmark:       155,712 ns RxDogTagAndroidPerf.observable_e2e[enabled=true,times=1,000]
benchmark:     2,953,542 ns RxDogTagAndroidPerf.flowable_simple[enabled=true,times=1,000]
benchmark:       163,333 ns RxDogTagAndroidPerf.flowable_e2e[enabled=true,times=1,000]
benchmark:     2,951,563 ns RxDogTagAndroidPerf.observable_simple[enabled=true,times=1,000]
benchmark:        26,559 ns RxDogTagAndroidPerf.observable_complex[enabled=true,times=1,000,000]
benchmark:        54,218 ns RxDogTagAndroidPerf.flowable_complex[enabled=true,times=1,000,000]
benchmark:    21,593,023 ns RxDogTagAndroidPerf.observable_e2e[enabled=true,times=1,000,000]
benchmark: 2,977,761,963 ns RxDogTagAndroidPerf.flowable_simple[enabled=true,times=1,000,000]
benchmark:    22,158,441 ns RxDogTagAndroidPerf.flowable_e2e[enabled=true,times=1,000,000]
benchmark: 3,041,113,168 ns RxDogTagAndroidPerf.observable_simple[enabled=true,times=1,000,000]
benchmark:         8,947 ns RxDogTagAndroidPerf.observable_complex[enabled=false,times=0]
benchmark:        12,039 ns RxDogTagAndroidPerf.flowable_complex[enabled=false,times=0]
benchmark:        77,994 ns RxDogTagAndroidPerf.observable_e2e[enabled=false,times=0]
benchmark:           140 ns RxDogTagAndroidPerf.flowable_simple[enabled=false,times=0]
benchmark:       115,208 ns RxDogTagAndroidPerf.flowable_e2e[enabled=false,times=0]
benchmark:           115 ns RxDogTagAndroidPerf.observable_simple[enabled=false,times=0]
benchmark:         1,719 ns RxDogTagAndroidPerf.observable_complex[enabled=false,times=1]
benchmark:        43,430 ns RxDogTagAndroidPerf.flowable_complex[enabled=false,times=1]
benchmark:       137,110 ns RxDogTagAndroidPerf.observable_e2e[enabled=false,times=1]
benchmark:           377 ns RxDogTagAndroidPerf.flowable_simple[enabled=false,times=1]
benchmark:       482,968 ns RxDogTagAndroidPerf.flowable_e2e[enabled=false,times=1]
benchmark:           396 ns RxDogTagAndroidPerf.observable_simple[enabled=false,times=1]
benchmark:         2,864 ns RxDogTagAndroidPerf.observable_complex[enabled=false,times=1,000]
  """.trimIndent()

  // Skip the header line
  val results = data.lineSequence()
      .filter { it.startsWith("benchmark") }
      .map { line ->
        // benchmark:     6,154,949 ns SpeedTest.gson_autovalue_buffer_fromJson_minified
        val (_, score, units, benchmark) = line.split("\\s+".toRegex())
        Analysis(
            benchmark = benchmark,
            score = score.replace(",", "").toLong(),
            units = units
        )
      }
      .toList()

  ResultType.values().forEach { printResults(it, results) }
}

private fun printResults(type: ResultType, results: List<Analysis>) {
  val groupedResults = type.groupings.associate { grouping ->
    grouping to results.filter {
      grouping.matchFunction(it.benchmark)
    }
  }
  val benchmarkLength = results.maxBy { it.benchmark.length }!!.benchmark.length
  val scoreLength = results.maxBy { it.formattedScore.length }!!.formattedScore.length

  val output = buildString {
    appendln()
    append(type.description)
    appendln(':')
    appendln()
    appendln("```")
    groupedResults.entries
        .joinTo(this, "\n\n", postfix = "\n```") { (grouping, matchedAnalyses) ->
          val sorted = matchedAnalyses.sortedBy { it.score }
          val first = sorted[0]
          val largestDelta = sorted.drop(1)
              .map {
                val delta = ((it.score - first.score).toDouble() / first.score) * 100
                String.format(Locale.US, "%.2f", delta)
              }
              .maxBy {
                it.length
              }!!
              .length
          val msLength = matchedAnalyses.map { String.format(Locale.US, "%.3f", it.score.toFloat() / 1000000) }
              .maxBy { it.length }!!
              .length
          val content = sorted
              .withIndex()
              .joinToString("\n") { (index, analysis) ->
                analysis.formattedString(benchmarkLength,
                    scoreLength,
                    largestDelta,
                    msLength,
                    if (index == 0) null else first.score)
              }
          "${grouping.name}\n$content"
        }
  }

  println(output)
}

private fun String.isFlowable(): Boolean = "flowable" in this
private fun String.isObservable(): Boolean = "observable" in this
private fun String.isSubscribeThroughput(): Boolean = "times=0" in this

private enum class ResultType(val description: String, val groupings: List<Grouping>) {
  THROUGHPUT(
      description = "Event throughput: grouped by number of events",
      groupings = listOf(
          Grouping("1 item (observable)") {
            "=1]" in it && it.isObservable()
          },
          Grouping("1 item (flowable)") {
            "=1]" in it && it.isFlowable()
          },
          Grouping("1000 items (observable)") {
            "=1,000]" in it && it.isObservable()
          },
          Grouping("1000 items (flowable)") {
            "=1,000]" in it && it.isFlowable()
          },
          Grouping("1000000 items (observable)") {
            "=1,000,000]" in it && it.isObservable()
          },
          Grouping("1000000 items (flowable)") {
            "=1,000,000]" in it && it.isFlowable()
          }
      )
  ),
  SUBSCRIBE(
      description = "Subscribe cost: grouped by complexity",
      groupings = listOf(
          Grouping("Simple (observable)") {
            "simple" in it && it.isSubscribeThroughput() && it.isObservable()
          },
          Grouping("Simple (flowable)") {
            "simple" in it && it.isSubscribeThroughput() && it.isFlowable()
          },
          Grouping("Complex (observable)") {
            "complex" in it && it.isSubscribeThroughput() && it.isObservable()
          },
          Grouping("Complex (flowable)") {
            "complex" in it && it.isSubscribeThroughput() && it.isFlowable()
          },
          Grouping("Complex (observable)") {
            "e2e" in it && it.isSubscribeThroughput() && it.isObservable()
          },
          Grouping("Complex (flowable)") {
            "e2e" in it && it.isSubscribeThroughput() && it.isFlowable()
          }
      )
  ),
  E2E(
      description = "E2E amortized cost",
      groupings = listOf(
          Grouping("Observable") {
            "e2e" in it && it.isObservable()
          },
          Grouping("Flowable") {
            "e2e" in it && it.isFlowable()
          }
      )
  )
}

private data class Grouping(
    val name: String,
    val matchFunction: (String) -> Boolean
)

private data class Analysis(
    val benchmark: String,
    val score: Long,
    val units: String
) {
  override fun toString() = "$benchmark\t$score\t$units"

  fun formattedString(benchmarkLength: Int, scoreLength: Int, msLength: Int, deltaLength: Int, base: Long?): String {
    return if (base == null) {
      String.format(Locale.US,
          "%-${benchmarkLength}s  %${scoreLength}s%s  %$msLength.3f%s",
          benchmark,
          formattedScore,
          units,
          score.toFloat() / 1000000,
          "ms")
    } else {
      val delta = ((score - base).toDouble() / base) * 100
      String.format(Locale.US,
          "%-${benchmarkLength}s  %${scoreLength}s%s  %$msLength.3f%s  %$deltaLength.2f%%",
          benchmark,
          formattedScore,
          units,
          score.toFloat() / 1000000,
          "ms",
          delta)
    }
  }

  val formattedScore: String
    get() = String.format(Locale.US, "%,d", score)
}
