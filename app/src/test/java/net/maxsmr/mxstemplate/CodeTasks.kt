package net.maxsmr.mxstemplate

import net.maxsmr.mxstemplate.CodeTasks.ListNode.Companion.toReversedListNode
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.assertEquals

class CodeTasks {

    @Test
    fun testRevertString() {
        val s = "qwerty1 qwerty2"
        assertEquals(revertString(s), s.reversed())
    }

    fun revertString(s: String): String {
        if (s.isEmpty()) return s
        val result = StringBuilder()
        for (i in s.length - 1 downTo 0) {
            result.append(s[i])
        }
        return result.toString()
    }

    @Test
    fun testArraySumOf() {
        val result = arraySumOf(arrayOf(1, 2, 3), arrayOf(4, 5, 6), 7)
        assertEquals(result, 0 to 2)
    }

    fun arraySumOf(array1: Array<Int>, array2: Array<Int>, target: Int): Pair<Int, Int>? {
        array1.forEachIndexed { index1, v1 ->
            array2.forEachIndexed { index2, v2 ->
                if (v1 + v2 == target) {
                    return Pair(index1, index2)
                }
            }
        }
        return null
    }

    @Test
    fun testMoveZerosToEnd() {
        assertEquals(
            moveZerosToEnd(intArrayOf(5, 4, 0, 3, 2, 0, 9)).toList(),
            intArrayOf(5, 4, 3, 2, 9, 0, 0).toList()
        )
    }

    fun moveZerosToEnd(array: IntArray): IntArray {
        val result = mutableListOf<Int>()
        val zeros = mutableListOf<Int>()
        array.forEach {
            if (it != 0) {
                result.add(it)
            } else {
                zeros.add(it)
            }
        }
        result.addAll(zeros)
        return result.toIntArray()
    }

    @Test
    fun testFindClosestDiffs() {
        assertEquals(intArrayOf(5, 7).toList(), findClosestDiffs(intArrayOf(2, 3, 5, 7, 11), 3, 2).toList())
        assertEquals(intArrayOf(12, 15, 15).toList(), findClosestDiffs(intArrayOf(4, 12, 15, 15, 24), 1, 3).toList())
    }

    fun findClosestDiffs(array: IntArray, index: Int, k: Int): IntArray {
        require(k >= 0)
        val target = array[index]
        val diffs = array.mapIndexed { i, v ->
            Pair(abs(v - target), i)
        }
        return diffs
            .sortedBy { it.first }
            .subList(0, min(k, diffs.size))
            .map { array[it.second] }
            .sorted()
            .toIntArray()
    }

    fun <T> reverseArray(array: Array<T>): Array<T> {
        val result = array.copyOf()
        for (i in array.size downTo 0) {
            result[i] = array[i]
        }
        return result
    }

    fun charCount(value: String): String {
        val countMap = mutableMapOf<Char, Int>()
        value.forEach {
            var count = countMap.getOrDefault(it, 0)
            count++
            countMap[it] = count
        }
        val result = StringBuilder()
        countMap.entries.forEach {
            result.append("${it.key}: ${it.value}")
        }
        return result.toString()
    }

    fun <T : Comparable<T>> sortArray(arr: Array<T>): Array<T> {
        for (i in 0 until arr.size - 1) {
            for (j in i + 1 until arr.size) {
                if (arr[i] > arr[j]) {
                    val temp: T = arr[i]
                    arr[i] = arr[j]
                    arr[j] = temp
                }
            }
        }
        return arr
    }

    fun <T : Comparable<T>> bubbleSortArray(arr: Array<T>): Array<T> {
        for (i in arr.size - 1 downUntil 1) {
            for (j in 0 until i) {
                if (arr[j] > arr[j + 1]) {
                    val tmp = arr[j]
                    arr[j] = arr[j + 1]
                    arr[j + 1] = tmp
                }
            }
        }
        return arr
    }

    infix fun Int.downUntil(to: Int): IntProgression {
        if (to >= Int.MAX_VALUE) return IntRange.EMPTY
        return this downTo (to + 1)
    }

    fun <T : Comparable<T>> mergeArraysWithSort(arr1: Array<T>, arr2: Array<T>): Array<T> {
        val result = arr1.copyOf(arr1.size + arr2.size)
        arr2.forEachIndexed { i, v ->
            result[arr1.size + i] = v
        }
        return sortArray(result as Array<T>)
    }

    fun containsDuplicate(nums: IntArray): Boolean {
        val sortedNums = nums.sortedArray()
        sortedNums.forEachIndexed { i, n ->
            if (n == sortedNums.getOrNull(i + 1)) {
                return true
            }
        }
        return false
    }

    @Test
    fun testAddTwoNumbers() {
        val first = ListNode(9)
        val second = ListNode(1)
        var current = second
        repeat(9) {
            val next = ListNode(9)
            current.next = next
            current = next
        }
        val result = addTwoNumbers(first, second)
        Assert.assertEquals(11, result?.toList()?.count())
    }

    class ListNode(var value: Int = -1, var next: ListNode? = null) {

        fun toList(): List<Int> {
            val result = mutableListOf<Int>()
            var nextNode: ListNode? = this
            while (nextNode != null) {
                result.add(nextNode.value)
                nextNode = nextNode.next
            }
            return result
        }

        fun toReversedList(): List<Int> {
            return toList().reversed()
        }

        override fun toString(): String {
            return "$value -> ${next.toString()}"
        }

        companion object {

            fun List<Int>.toReversedListNode(): ListNode? {
                var currentNode: ListNode? = null
                var nextNode: ListNode? = null
                this.forEach {
                    nextNode = ListNode(it, nextNode)
                    currentNode = nextNode
                }
                return currentNode
            }
        }
    }

    fun addTwoNumbers(l1: ListNode?, l2: ListNode?): ListNode? {

        val firstList = l1?.toReversedList().orEmpty()
        val secondList = l2?.toReversedList().orEmpty()

        val firstNumber: BigDecimal = firstList.joinToString("").toBigDecimalOrNull() ?: BigDecimal(0)
        val secondNumber: BigDecimal = secondList.joinToString("").toBigDecimalOrNull() ?: BigDecimal(0)

        val sum: BigDecimal = firstNumber.add(secondNumber)
        val sumNumbers = mutableListOf<Int>()
        sum.toString().map {
            it.digitToInt().let { num ->
                sumNumbers.add(num)
            }
        }

        return sumNumbers.toReversedListNode()
    }

    fun mergeTwoLists(list1: ListNode?, list2: ListNode?): ListNode? {

        val result = list1?.toList()?.toMutableList() ?: mutableListOf()
        list2?.toList()?.let {
            result.addAll(it)
        }

        result.sortDescending()
        return result.toReversedListNode()
    }

    fun lengthOfLongestUniqueSubstring(s: String): Int {
        var maxSubstringCount = 0
        val currentUniqueChars = mutableListOf<Char>()

        fun setMaxWithClear() {
            if (currentUniqueChars.size > maxSubstringCount) {
                maxSubstringCount = currentUniqueChars.size
            }
            currentUniqueChars.clear()
        }

        for (index in s.indices) {
            for (currentIndex in index until s.length) {
                val currentChar = s[currentIndex]
                if (currentUniqueChars.contains(currentChar)) {
                    setMaxWithClear()
                }
                currentUniqueChars.add(currentChar)
//            previousChar?.let {
//                if (it != currentChar) {
//                    currentCount++
//                } else {
//                    maxSubstringCounts.add(currentCount)
//                    currentCount = 0
//                }
//            }
//            previousChar = currentChar
            }
            setMaxWithClear()
        }
        return maxSubstringCount
    }

    @Test
    fun testLengthOfLongestUniqueSubstring() {
        assertEquals(0, lengthOfLongestUniqueSubstring("", 0))
        assertEquals(6, lengthOfLongestUniqueSubstring("eeegbbc", 3))
        assertEquals(3, lengthOfLongestUniqueSubstring("eceba", 2))
        assertEquals(6, lengthOfLongestUniqueSubstring("ecebbbdfhhhhfah", 3))
        assertEquals(5, lengthOfLongestNotUniqueSubstring("ecebae", 2))
        assertEquals(10, lengthOfLongestNotUniqueSubstring("eccesdgsdggg", 2))
        assertEquals(11, lengthOfLongestNotUniqueSubstring("ecebbbdfhhhhfah", 3))
        assertEquals(6, lengthOfLongestUniqueSubstring("asjrgapa"))
    }

    /**
     * Для заданной строки s и целого числа k вернуть длину самой длинной подстроки s, содержащей не более k различных символов.
     *
     * Input: s = "eceba", k = 2
     *
     * Output: 3 ("ece")
     *
     * Input: s = "ecebbbdfhhhhfah", k = 3
     *
     * Output: 6 "ecebbb"
     */
    fun lengthOfLongestUniqueSubstring(s: String, k: Int): Int {
        require(k >= 0)

        var result = ""
        if (k > 0) {
            for (index in s.indices) {
                val set = mutableSetOf<Char>()
                for (i1 in 0..index) {
                    val c = s[i1]
                    for (i2 in 0..index) {
                        if (c == s[i2]) {
                            // текущий символ в подстроке до index
                            set.add(c)
                        }
                    }
                }
                if (set.size <= k) {
                    val subst = s.substring(0, index + 1)
                    if (subst.length > result.length) {
                        result = subst
                    }
                } else {
                    break
                }
            }
        }
        return result.length
    }

    /**
     * Для заданной строки s и целого числа k вернуть длину самой длинной подстроки s, содержащей не более k одинаковых символов.
     *
     * Input: s = "eceba", k = 2
     *
     * Output: 4 ("ceba")
     *
     * Input: s = "eccesdgsdggg", k = 2
     *
     * Output: 8 ("cesdgsdg")
     *
     * Input: s = "ecebbbdfhhhhfah", k = 3
     *
     * Output: 11 "ecebbbdfhhh"
     */
    fun lengthOfLongestNotUniqueSubstring(s: String, k: Int): Int {
        require(k >= 0)

        var result = ""
        if (k > 0) {
            for (index in s.indices) {
                val map = mutableMapOf<Char, Int>()
                for (i1 in 0..index) {
                    val c = s[i1]
                    // количество вхождений символа c в текущую подстроку до index
                    var count = 0
                    for (i2 in 0..index) {
                        if (c == s[i2]) {
                            count++
                        }
                    }
                    map[c] = count
                }
                if (map.values.max() <= k) {
                    val subst = s.substring(0, index + 1)
                    if (subst.length > result.length) {
                        result = subst
                    }
                } else {
                    break
                }
            }
        }
        return result.length
    }

    fun numbers(): List<Int> {
        return (0..1000)
            .toList()
            .filter {
                it % 3 == 0
                        && it % 5 != 0
                        && it.toString().chars().map { c ->
                    c.toChar().digitToInt()
                }.sum() < 10
            }
    }

    @Test
    fun testSuggestMostUsable() {
        assertEquals(
            's', suggestMostUsable(
                "sayHello",
                "println",
                "sleep",
                "spendMoney",
                "log"
            )
        )
    }

    fun suggestMostUsable(vararg commands: String): Char? {
        val map = mutableMapOf<Char, Int>()
        for (s in commands) {
            if (s.isEmpty()) return null
            val c = s.getOrNull(0) ?: continue
            if (map.contains(c)) continue
            map[c] = commands.count { c == it.getOrNull(0) }
        }
        return map.entries.maxBy { it.value }.key
    }

    @Test
    fun testTimeDiff() {
        assertEquals(
            "1:05", timeDiff(
                "12:00",
                "13:05",
                0
            )
        )

        assertEquals(
            "5:50", timeDiff(
                "22:30",
                "04:20",
                0
            )
        )

        assertEquals(
            "23:50", timeDiff(
                "00:30",
                "00:20",
                0
            )
        )

        assertEquals(
            "10:50", timeDiff(
                "1:00",
                "12:50",
                1
            )
        )
    }

    // TODO utils with TimeUnit
    fun timeDiff(startTimeStr: String, endTimeStr: String, timeZoneDiff: Int): String? {
        require(timeZoneDiff in -12..14)
        fun String.getHoursWithMinutes(): Pair<Int, Int>? {
            val parts = split(":").takeIf { it.size == 2 } ?: return null
            return parts[0].toInt() to parts[1].toInt()
        }

        val startTime = startTimeStr.getHoursWithMinutes() ?: return null
        val endTime = endTimeStr.getHoursWithMinutes() ?: return null

        val (startHours, startMinutes) = startTime
        val (endHours, endMinutes) = endTime.first - timeZoneDiff to endTime.second
        var diffHours = if (endHours > startHours) {
            endHours - startHours
        } else {
            (24 - startHours) + endHours
        }
        val diffMinutes = if (endMinutes > startMinutes) {
            endMinutes - startMinutes
        } else {
            (60 - startMinutes) + endMinutes.also {
                diffHours -= 1
            }
        }
        return "$diffHours:${if (diffMinutes.toString().length > 1) diffMinutes else "0$diffMinutes"}"
    }

    fun sumUnique(arr: IntArray): Int {
        val set = arr.toSet()
        return set.sum()
    }

    @Test
    fun testIsPalindrome() {
        assertEquals(true, isPalindrome("404"))
        assertEquals(true, isPalindrome("saippuakivikauppias"))
        assertEquals(false, isPalindrome("abcvbdfgkln"))
    }

    fun isPalindrome(str: String): Boolean {
        if (str.length <= 2) return false
        val endIndex = (str.length / 2f).toInt()
        val firstPart = str.substring(0, endIndex)
        val secondPart = str.substring(endIndex + 1, str.length).reversed()
        return firstPart == secondPart
    }

    @Test
    fun testConvertToRle() {
        assertEquals("A5B3CD4", convertToRle("AAAAABBBCDDDD"))
    }

    fun convertToRle(s: String): String {
        val result = StringBuilder()
        var lastRepeatChar: Char? = null
        var lastRepeatCount = 0
        s.forEachIndexed { i, c ->
            if (lastRepeatChar == null) {
                lastRepeatChar = c
            }
            if (lastRepeatChar == c) {
                lastRepeatCount++
            } else {
                result.append(s[i - 1])
                if (lastRepeatCount > 1) {
                    result.append(lastRepeatCount)
                }
                lastRepeatCount = 1
                lastRepeatChar = c
            }
        }
        if (lastRepeatCount > 0) {
            result.append(s.last())
            result.append(lastRepeatCount)
        }
        return result.toString()
    }

    @Test
    fun testCountRleChars() {
        assertEquals(21, countRleChars("A15BA5"))
        assertEquals(5, countRleChars("ABCDR"))
        assertEquals(125, countRleChars("Z123XY"))
        assertEquals(6, countRleChars("ABCDEF"))
    }

    fun countRleChars(s: String): Int {
        var currentChar: Char? = null
        val currentRepeatCountSubstring = StringBuilder()
        var result = 0

        fun appendCount(isFirst: Boolean) {
            val count = if (currentRepeatCountSubstring.isNotEmpty()) {
                currentRepeatCountSubstring.toString().toInt().also {
                    currentRepeatCountSubstring.clear()
                }
            } else {
                if (!isFirst) {
                    1
                } else {
                    0
                }
            }
            result += count
        }

        s.forEachIndexed { i, c ->
            if (!c.isDigit()) {
                appendCount(i == 0)
                currentChar = c
            } else if (currentChar != null) {
                currentRepeatCountSubstring.append(c)
            } else {
                result++
            }
        }
        appendCount(false)
        return result
    }

    @Test
    fun testFormatIntArray() {
        assertEquals("10-12,30,40", formatIntArray(intArrayOf(10, 30, 12, 40, 11)))
        assertEquals("1-3,5-7,10", formatIntArray(intArrayOf(1, 7, 5, 6, 10, 2, 3)))
    }

    fun formatIntArray(array: IntArray): String {
        val result = mutableListOf<String>()
        val current = mutableListOf<Int>()

        fun addFromCurrent() {
            val first = current.first()
            if (current.size >= 2) {
                val last = current.last()
                result.add("$first-$last")
            } else {
                result.add(first.toString())
            }
        }

        for (value in array.sortedArray()) {
            if (value < 0) continue
            if (current.isEmpty() || value - current.last() == 1) {
                current.add(value)
            } else {
                addFromCurrent()
                current.clear()
                current.add(value)
            }
        }
        if (current.isNotEmpty()) {
            addFromCurrent()
        }

        return result.joinToString(",")
    }

    fun formatIntArray2(array: IntArray): String {
        val input = array.toMutableSet()
        val result = StringBuilder()
        while (input.isNotEmpty()) {
            val item = input.first()
            input.remove(item)
            var start = item
            var end = item

            var prevItem = item - 1
            while (input.contains(prevItem)) {
                input.remove(prevItem)
                start = prevItem
                prevItem--
            }

            var nextItem = item + 1
            while (input.contains(nextItem)) {
                input.remove(nextItem)
                end = nextItem
                nextItem++
            }

            if (start == end) {
                result.append(start.toString())
            } else {
                result.append("$start-$end")
            }
            if (input.isNotEmpty()) {
                result.append(",")
            }
        }
        return result.toString()
    }

    @Test
    fun testSubSequenceOf() {
        assertEquals(
            listOf(1, 2, 3),
            subSequenceOf(intArrayOf(1, 2, 3), intArrayOf(1, 2, 3)).toList()
        )
        assertEquals(
            listOf(1, 2, 3),
            subSequenceOf(intArrayOf(1, 2, 3, 4, 5), intArrayOf(6, 1, 2, 3, 7, 8)).toList()
        )
        assertEquals(
            listOf(2, 3),
            subSequenceOf(intArrayOf(1, 2, 3), intArrayOf(2, 3, 1)).toList()
        )
        assertEquals(
            listOf(3, 9, 5),
            subSequenceOf(intArrayOf(4, 7, 1, 2, 3, 9, 5), intArrayOf(5, 8, 4, 6, 1, 9, 1, 2, 5, 3, 9, 5)).toList()
        )
        assertEquals(
            listOf(1),
            subSequenceOf(intArrayOf(1), intArrayOf(1)).toList()
        )
        assertEquals(
            listOf(),
            subSequenceOf(intArrayOf(), intArrayOf()).toList()
        )
    }

    fun subSequenceOf(array1: IntArray, array2: IntArray): IntArray {
        val currentSubSeq = mutableListOf<Int>()
        val allSubSeq = mutableListOf<List<Int>>()
        var currentIndex1 = 0
        var currentIndex2 = 0

        fun appendCurrent(): Boolean {
            if (currentSubSeq.isNotEmpty()) {
                allSubSeq.add(currentSubSeq.toList())
                currentSubSeq.clear()
                currentIndex1++
                return true
            }
            return false
        }

        while (currentIndex1 < array1.size && currentIndex2 < array2.size) {
            run label@{
                (currentIndex1 until array1.size).forEach { i1 ->
                    val a1 = array1[i1]
                    if (currentIndex2 < array2.size) {
                        for (i2 in currentIndex2 until array2.size) {
                            if (a1 == array2[i2]) {
                                currentSubSeq.add(a1)
                                currentIndex2 = i2 + 1
                                // выход из вложенного цикла
                                break
                            } else if (appendCurrent()) {
                                // выход по метке из forEach и возврат в while
                                return@label
                            }
                        }
                        if (currentSubSeq.isEmpty()) {
                            // за эту итерацию по array1 не собрали последовательность
                            currentIndex1++
                        }
                    }
                    if (currentIndex2 == array2.size) {
                        appendCurrent()
                        currentIndex2 = 0
                    }
                }
            }
        }
        return allSubSeq.maxByOrNull { it.size }?.toIntArray() ?: intArrayOf()
    }

    @Test
    fun testFindPath() {
        println()

        val matrix1 = arrayOf(
            intArrayOf(1, 1, 1, 1, 1),
            intArrayOf(3, 100, 100, 100, 100),
            intArrayOf(1, 1, 1, 1, 1),
            intArrayOf(2, 2, 2, 2, 1),
            intArrayOf(1, 1, 1, 1, 1),
        )

        val matrix2 = arrayOf(
            intArrayOf(4, 7, 2, 3, 5),
            intArrayOf(8, 1, 1, 1, 1),
            intArrayOf(6, 4, 2, 2, 20),
            intArrayOf(5, 5, 1, 5, 5),
            intArrayOf(8, 8, 1, 8, 8),
        )

        assertEquals(
            11, findMinPath(matrix1).sum
        )
        assertEquals(
            407, findMaxPath(matrix1).sum
        )
        assertEquals(
            33, findMinPath(matrix2).sum
        )
        assertEquals(
            59, findMaxPath(matrix2).sum
        )
    }

    fun findMinPath(matrix: Array<IntArray>): PathResult {
        val result = findPathInternal(matrix, true)
        val size = matrix.sumOf { it.size }
        println(
            "Found min path sum ${result.sum} in matrix with size $size in ${result.iterationsCount} iterations, " +
                    "result route with ${result.route.size} point is ${result.route}"
        )
        return result
    }

    fun findMaxPath(matrix: Array<IntArray>): PathResult {
        val result = findPathInternal(matrix, false)
        val size = matrix.sumOf { it.size }
        println(
            "Found max path sum ${result.sum} in matrix with size $size in ${result.iterationsCount} iterations, " +
                    "result route with ${result.route.size} points is ${result.route}"
        )
        return result
    }

    private fun findPathInternal(
        matrix: Array<IntArray>,
        isMin: Boolean,
        initialRowIndex: Int = 0,
        initialColumnIndex: Int = 0,
//        targetCount: Int = matrix.sumOf { it.size },
        isRecursive: Boolean = true,
    ): PathResult {
        require(matrix.all { it.size == matrix[0].size }) { "matrix is not square" }

        var sum = 0
        var count = 0
        val route = mutableListOf<Pair<Int, Int>>()
        var rowIndex = initialRowIndex - 1
        var columnIndex = initialColumnIndex - 1

        infix fun Int.compare(other: Int): Boolean {
            return if (isMin) {
                this > other
            } else {
                other > this
            }
        }

        infix fun PathResult.compareAndApply(other: PathResult): Boolean {
            val result = if (isMin) {
                this.takeIf { it.sum > other.sum } to other
            } else {
                other.takeIf { it.sum > this.sum } to this
            }
            with(result.first ?: result.second) {
                count += this.iterationsCount
            }
            return result.first != null
        }

        while (/*count < targetCount &&*/ rowIndex < matrix.size - 1) {
            val rows = matrix[++rowIndex]
            if (columnIndex < rows.size - 1) {

                val current = matrix[rowIndex][++columnIndex]
                route.add(rowIndex to columnIndex)
                sum += current
                count++

                val nextInRow = matrix[rowIndex].getOrNull(columnIndex + 1)
//                        ?: matrix.getOrNull(rowIndex + 1)?.getOrNull(0)
                val nextInColumn = matrix.getOrNull(rowIndex + 1)?.getOrNull(columnIndex)
                if (nextInRow == null && nextInColumn == null) {
                    break
                } else {
                    val (nextRow, nextColumn) = if (nextInColumn == null) {
                        rowIndex - 1 to columnIndex
                    } else {
                        if (nextInRow == null
                                || if (isRecursive) {
                                    // или сумма последующих элементов по строке превышает суммму, по текущему столбцу;
                                    // сумму не накапливаем, только для сравнения
                                    findPathInternal(
                                        matrix,
                                        isMin,
                                        rowIndex,
                                        columnIndex + 1,
//                                            size - count,
                                        true
                                    ).compareAndApply(
                                        findPathInternal(
                                            matrix,
                                            isMin,
                                            rowIndex + 1,
                                            columnIndex,
//                                            size - count,
                                            true
                                        )
                                    ) // nextInColumn
                                } else {
                                    // без рекурсии - вместо суммы следующий элемент по строке превышает следующий по столбцу;
                                    // во многих комбинациях оказывается недостаточно
                                    nextInRow.compare(nextInColumn)
                                }
                        ) {
                            // отдать предпочтение следующему в столбце
                            rowIndex to columnIndex - 1
                        } else {
                            // отдать предпочтение следующему в строке
                            rowIndex - 1 to columnIndex
                        }
                    }
                    rowIndex = nextRow
                    columnIndex = nextColumn
                }
            }
        }
        return PathResult(sum, count, route)
    }

    data class PathResult(
        val sum: Int,
        val iterationsCount: Int,
        val route: List<Pair<Int, Int>>,
    )
}