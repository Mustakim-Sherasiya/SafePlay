//package com.chat.safeplay.games
//
//
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            SlidingPuzzleTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    SlidingPuzzle()
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun SlidingPuzzleTheme(content: @Composable () -> Unit) {
//    val colors = lightColorScheme(
//        primary = Color(0xFF6200EE),
//        onPrimary = Color.White,
//        secondary = Color(0xFF03DAC5),
//        onSecondary = Color.Black,
//        background = Color.White,
//        surface = Color.White,
//        onSurface = Color.Black
//    )
//
//    MaterialTheme(
//        colorScheme = colors,
//        typography = Typography(),
//        content = content
//    )
//}
//
//@Composable
//fun SlidingPuzzle() {
//    val gridSize = 3
//    var puzzle by remember { mutableStateOf(shufflePuzzle(gridSize)) }
//    val emptyIndex = puzzle.indexOf(0)
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            "Sliding Puzzle",
//            fontSize = 28.sp,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(bottom = 16.dp),
//            color = MaterialTheme.colorScheme.primary
//        )
//
//        for (row in 0 until gridSize) {
//            Row {
//                for (col in 0 until gridSize) {
//                    val index = row * gridSize + col
//                    val tile = puzzle[index]
//
//                    Tile(
//                        number = tile,
//                        onClick = {
//                            if (canMove(index, emptyIndex, gridSize)) {
//                                puzzle = puzzle.toMutableList().apply {
//                                    this[emptyIndex] = tile
//                                    this[index] = 0
//                                }
//                            }
//                        }
//                    )
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        if (isSolved(puzzle)) {
//            Text(
//                "Congratulations! You solved the puzzle!",
//                color = Color(0xFF388E3C),
//                fontWeight = FontWeight.Bold,
//                fontSize = 20.sp
//            )
//        } else {
//            Button(onClick = { puzzle = shufflePuzzle(gridSize) }) {
//                Text("Shuffle")
//            }
//        }
//    }
//}
//
//@Composable
//fun Tile(number: Int, onClick: () -> Unit) {
//    val tileColor =
//        if (number == 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
//
//    Box(
//        modifier = Modifier
//            .size(100.dp)
//            .padding(4.dp)
//            .background(color = tileColor, shape = RoundedCornerShape(8.dp))
//            .clickable(enabled = number != 0) { onClick() },
//        contentAlignment = Alignment.Center
//    ) {
//        if (number != 0) {
//            Text(
//                text = number.toString(),
//                fontSize = 32.sp,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onPrimary
//            )
//        }
//    }
//}
//
//fun canMove(clickedIndex: Int, emptyIndex: Int, gridSize: Int): Boolean {
//    val clickedRow = clickedIndex / gridSize
//    val clickedCol = clickedIndex % gridSize
//    val emptyRow = emptyIndex / gridSize
//    val emptyCol = emptyIndex % gridSize
//
//    val rowDiff = kotlin.math.abs(clickedRow - emptyRow)
//    val colDiff = kotlin.math.abs(clickedCol - emptyCol)
//
//    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
//}
//
//fun shufflePuzzle(gridSize: Int): List<Int> {
//    val list = (1 until gridSize * gridSize).toMutableList()
//    list.add(0)
//
//    do {
//        list.shuffle()
//    } while (!isSolvable(list, gridSize) || isSolved(list))
//
//    return list
//}
//
//fun isSolved(puzzle: List<Int>): Boolean {
//    for (i in 0 until puzzle.size - 1) {
//        if (puzzle[i] != i + 1) return false
//    }
//    return puzzle.last() == 0
//}
//
//fun isSolvable(puzzle: List<Int>, gridSize: Int): Boolean {
//    val inversionCount = countInversions(puzzle)
//    if (gridSize % 2 == 1) {
//        return inversionCount % 2 == 0
//    } else {
//        val emptyRow = puzzle.indexOf(0) / gridSize
//        val emptyRowFromBottom = gridSize - emptyRow
//        return (inversionCount + emptyRowFromBottom) % 2 == 0
//    }
//}
//
//fun countInversions(puzzle: List<Int>): Int {
//    val filtered = puzzle.filter { it != 0 }
//    var inversions = 0
//    for (i in filtered.indices) {
//        for (j in i + 1 until filtered.size) {
//            if (filtered[i] > filtered[j]) inversions++
//        }
//    }
//    return inversions
//}
