package com.example.androidapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.domain.CalculatorOperator

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Flat edge-to-edge display surface
        CalculatorDisplay(
            expression = uiState.expressionPreview,
            result = uiState.displayText,
            isError = uiState.isError,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Expressive mixed pill and rounded squircle keypad
        CalculatorKeypad(
            onDigitClick = { viewModel.onDigitClicked(it) },
            onDecimalClick = { viewModel.onDecimalClicked() },
            onOperatorClick = { viewModel.onOperatorClicked(it) },
            onEqualsClick = { viewModel.onEqualsClicked() },
            onClearClick = { viewModel.onClearClicked() }
        )
    }
}

@Composable
fun CalculatorDisplay(
    expression: String,
    result: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        if (expression.isNotEmpty()) {
            Text(
                text = expression,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    letterSpacing = (-0.25).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Expression: $expression" }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        val resultFontSize = when {
            result.length > 12 -> 34.sp
            result.length > 9 -> 46.sp
            result.length > 6 -> 60.sp
            else -> 76.sp
        }

        Text(
            text = result,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = resultFontSize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1.5).sp,
                lineHeight = (resultFontSize.value * 1.05).sp
            ),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = if (isError) "Error: $result" else "Result: $result" }
        )
    }
}

@Composable
fun CalculatorKeypad(
    onDigitClick: (Char) -> Unit,
    onDecimalClick: () -> Unit,
    onOperatorClick: (CalculatorOperator) -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: AC (spans 3 cols), ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalculatorButton(
                text = "AC",
                contentDescription = "Clear all",
                modifier = Modifier.weight(3f),
                buttonType = CalculatorButtonType.Action,
                onClick = onClearClick
            )
            CalculatorButton(
                text = "÷",
                contentDescription = "Division",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                onClick = { onOperatorClick(CalculatorOperator.DIVIDE) }
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalculatorButton(text = "7", contentDescription = "7", modifier = Modifier.weight(1f), onClick = { onDigitClick('7') })
            CalculatorButton(text = "8", contentDescription = "8", modifier = Modifier.weight(1f), onClick = { onDigitClick('8') })
            CalculatorButton(text = "9", contentDescription = "9", modifier = Modifier.weight(1f), onClick = { onDigitClick('9') })
            CalculatorButton(
                text = "×",
                contentDescription = "Multiplication",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                onClick = { onOperatorClick(CalculatorOperator.MULTIPLY) }
            )
        }

        // Row 3: 4, 5, 6, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalculatorButton(text = "4", contentDescription = "4", modifier = Modifier.weight(1f), onClick = { onDigitClick('4') })
            CalculatorButton(text = "5", contentDescription = "5", modifier = Modifier.weight(1f), onClick = { onDigitClick('5') })
            CalculatorButton(text = "6", contentDescription = "6", modifier = Modifier.weight(1f), onClick = { onDigitClick('6') })
            CalculatorButton(
                text = "-",
                contentDescription = "Subtraction",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                onClick = { onOperatorClick(CalculatorOperator.SUBTRACT) }
            )
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalculatorButton(text = "1", contentDescription = "1", modifier = Modifier.weight(1f), onClick = { onDigitClick('1') })
            CalculatorButton(text = "2", contentDescription = "2", modifier = Modifier.weight(1f), onClick = { onDigitClick('2') })
            CalculatorButton(text = "3", contentDescription = "3", modifier = Modifier.weight(1f), onClick = { onDigitClick('3') })
            CalculatorButton(
                text = "+",
                contentDescription = "Addition",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                onClick = { onOperatorClick(CalculatorOperator.ADD) }
            )
        }

        // Row 5: 0 (spans 2 cols), ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalculatorButton(text = "0", contentDescription = "0", modifier = Modifier.weight(2f), onClick = { onDigitClick('0') })
            CalculatorButton(text = ".", contentDescription = "Decimal point", modifier = Modifier.weight(1f), onClick = onDecimalClick)
            CalculatorButton(
                text = "=",
                contentDescription = "Equals",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Equals,
                onClick = onEqualsClick
            )
        }
    }
}

enum class CalculatorButtonType {
    Numeric,
    Operator,
    Action,
    Equals
}

@Composable
fun CalculatorButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonType: CalculatorButtonType = CalculatorButtonType.Numeric,
    onClick: () -> Unit
) {
    val shape = when (buttonType) {
        CalculatorButtonType.Numeric -> RoundedCornerShape(26.dp)
        CalculatorButtonType.Operator, CalculatorButtonType.Action, CalculatorButtonType.Equals -> RoundedCornerShape(percent = 50)
    }

    val colors = when (buttonType) {
        CalculatorButtonType.Action -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalculatorButtonType.Equals -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        CalculatorButtonType.Operator -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        CalculatorButtonType.Numeric -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val textStyle = when (buttonType) {
        CalculatorButtonType.Action -> MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            letterSpacing = 0.5.sp
        )
        CalculatorButtonType.Operator -> MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp
        )
        CalculatorButtonType.Equals -> MaterialTheme.typography.headlineLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp
        )
        CalculatorButtonType.Numeric -> MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(74.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = shape,
        colors = colors,
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center
        )
    }
}
