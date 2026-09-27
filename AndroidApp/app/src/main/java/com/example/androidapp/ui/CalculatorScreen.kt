package com.example.androidapp.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.domain.CalculatorOperator

/**
 * Phone-first ergonomic dimension scaling for Material 3 Expressive Calculator.
 */
data class CalculatorDimensions(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val gridSpacing: Dp,
    val buttonHeight: Dp,
    val numericCornerRadius: Dp,
    val displayToKeypadGap: Dp,
    val expressionFontSize: TextUnit,
    val maxResultFontSize: TextUnit
)

@Composable
fun rememberCalculatorDimensions(): CalculatorDimensions {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    return remember(screenWidth, screenHeight) {
        when {
            // Compact phone (e.g. width < 380dp)
            screenWidth < 380 -> CalculatorDimensions(
                horizontalPadding = 12.dp,
                verticalPadding = 10.dp,
                gridSpacing = 10.dp,
                buttonHeight = 66.dp,
                numericCornerRadius = 22.dp,
                displayToKeypadGap = 16.dp,
                expressionFontSize = 20.sp,
                maxResultFontSize = 64.sp
            )
            // Large phone / Phablet (e.g. width > 420dp)
            screenWidth > 420 -> CalculatorDimensions(
                horizontalPadding = 18.dp,
                verticalPadding = 16.dp,
                gridSpacing = 14.dp,
                buttonHeight = 78.dp,
                numericCornerRadius = 28.dp,
                displayToKeypadGap = 24.dp,
                expressionFontSize = 24.sp,
                maxResultFontSize = 80.sp
            )
            // Standard phone (380dp - 420dp, Pixel 8, Galaxy S24)
            else -> CalculatorDimensions(
                horizontalPadding = 16.dp,
                verticalPadding = 14.dp,
                gridSpacing = 12.dp,
                buttonHeight = 72.dp,
                numericCornerRadius = 26.dp,
                displayToKeypadGap = 20.dp,
                expressionFontSize = 22.sp,
                maxResultFontSize = 74.sp
            )
        }
    }
}

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dimensions = rememberCalculatorDimensions()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = dimensions.horizontalPadding, vertical = dimensions.verticalPadding),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Flat containerless dual-tier display
        CalculatorDisplay(
            uiState = uiState,
            dimensions = dimensions,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(dimensions.displayToKeypadGap))

        // Symmetric 4x5 Google Calculator keypad
        CalculatorKeypad(
            dimensions = dimensions,
            onDigitClick = { viewModel.onDigitClicked(it) },
            onDecimalClick = { viewModel.onDecimalClicked() },
            onOperatorClick = { viewModel.onOperatorClicked(it) },
            onParenthesisClick = { viewModel.onParenthesisClicked() },
            onPercentClick = { viewModel.onPercentClicked() },
            onEqualsClick = { viewModel.onEqualsClicked() },
            onClearClick = { viewModel.onClearClicked() },
            onBackspaceClick = { viewModel.onBackspaceClicked() }
        )
    }
}

private fun calculateDynamicFontSize(text: String, baseSize: TextUnit): TextUnit {
    return when {
        text.length > 20 -> (baseSize.value * 0.45).sp
        text.length > 15 -> (baseSize.value * 0.58).sp
        text.length > 11 -> (baseSize.value * 0.72).sp
        text.length > 8 -> (baseSize.value * 0.85).sp
        else -> baseSize
    }
}

@Composable
fun CalculatorDisplay(
    uiState: CalculatorUiState,
    dimensions: CalculatorDimensions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        val isCommitted = uiState.isCommitted
        val hasPreview = !isCommitted && uiState.liveResult.isNotEmpty()

        val topText = when {
            isCommitted -> uiState.liveExpression.ifEmpty { uiState.expressionPreview }
            uiState.liveExpression.isNotEmpty() -> uiState.liveExpression
            else -> uiState.displayText
        }

        val bottomText = when {
            isCommitted -> uiState.displayText
            hasPreview -> uiState.liveResult
            else -> ""
        }

        // Top line
        if (isCommitted) {
            if (topText.isNotEmpty()) {
                Text(
                    text = topText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = dimensions.expressionFontSize,
                        letterSpacing = (-0.25).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Expression: $topText" }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        } else {
            // Live active expression line
            val activeFontSize = if (hasPreview) {
                calculateDynamicFontSize(topText, (dimensions.maxResultFontSize.value * 0.65).sp)
            } else {
                calculateDynamicFontSize(topText, dimensions.maxResultFontSize)
            }

            Text(
                text = topText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = activeFontSize,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.2).sp,
                    lineHeight = (activeFontSize.value * 1.05).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Active expression: $topText" }
            )
        }

        // Bottom line (Hero result or live preview)
        if (isCommitted) {
            val resultFontSize = calculateDynamicFontSize(bottomText, dimensions.maxResultFontSize)
            Text(
                text = bottomText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = resultFontSize,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.5).sp,
                    lineHeight = (resultFontSize.value * 1.05).sp
                ),
                color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = if (uiState.isError) "Error: $bottomText" else "Result: $bottomText" }
            )
        } else if (hasPreview) {
            Spacer(modifier = Modifier.height(4.dp))
            val previewFontSize = calculateDynamicFontSize(bottomText, (dimensions.maxResultFontSize.value * 0.52).sp)
            Text(
                text = bottomText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = previewFontSize,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Preview result: $bottomText" }
            )
        }
    }
}

@Composable
fun CalculatorKeypad(
    dimensions: CalculatorDimensions,
    onDigitClick: (Char) -> Unit,
    onDecimalClick: () -> Unit,
    onOperatorClick: (CalculatorOperator) -> Unit,
    onParenthesisClick: () -> Unit,
    onPercentClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.gridSpacing)
    ) {
        // Row 1: AC, ( ), %, ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridSpacing)
        ) {
            CalculatorButton(
                text = "AC",
                contentDescription = "Clear all",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Function,
                height = dimensions.buttonHeight,
                onClick = onClearClick
            )
            CalculatorButton(
                text = "( )",
                contentDescription = "Parentheses",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Function,
                height = dimensions.buttonHeight,
                onClick = onParenthesisClick
            )
            CalculatorButton(
                text = "%",
                contentDescription = "Percentage",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Function,
                height = dimensions.buttonHeight,
                onClick = onPercentClick
            )
            CalculatorButton(
                text = "÷",
                contentDescription = "Division",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                height = dimensions.buttonHeight,
                onClick = { onOperatorClick(CalculatorOperator.DIVIDE) }
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridSpacing)
        ) {
            CalculatorButton(text = "7", contentDescription = "7", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('7') })
            CalculatorButton(text = "8", contentDescription = "8", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('8') })
            CalculatorButton(text = "9", contentDescription = "9", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('9') })
            CalculatorButton(
                text = "×",
                contentDescription = "Multiplication",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                height = dimensions.buttonHeight,
                onClick = { onOperatorClick(CalculatorOperator.MULTIPLY) }
            )
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridSpacing)
        ) {
            CalculatorButton(text = "4", contentDescription = "4", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('4') })
            CalculatorButton(text = "5", contentDescription = "5", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('5') })
            CalculatorButton(text = "6", contentDescription = "6", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('6') })
            CalculatorButton(
                text = "−",
                contentDescription = "Subtraction",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                height = dimensions.buttonHeight,
                onClick = { onOperatorClick(CalculatorOperator.SUBTRACT) }
            )
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridSpacing)
        ) {
            CalculatorButton(text = "1", contentDescription = "1", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('1') })
            CalculatorButton(text = "2", contentDescription = "2", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('2') })
            CalculatorButton(text = "3", contentDescription = "3", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('3') })
            CalculatorButton(
                text = "+",
                contentDescription = "Addition",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Operator,
                height = dimensions.buttonHeight,
                onClick = { onOperatorClick(CalculatorOperator.ADD) }
            )
        }

        // Row 5: 0, ., ⌫, =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.gridSpacing)
        ) {
            CalculatorButton(text = "0", contentDescription = "0", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = { onDigitClick('0') })
            CalculatorButton(text = ".", contentDescription = "Decimal point", modifier = Modifier.weight(1f), height = dimensions.buttonHeight, onClick = onDecimalClick)
            CalculatorButton(
                text = "⌫",
                contentDescription = "Backspace",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Function,
                height = dimensions.buttonHeight,
                onClick = onBackspaceClick
            )
            CalculatorButton(
                text = "=",
                contentDescription = "Equals",
                modifier = Modifier.weight(1f),
                buttonType = CalculatorButtonType.Equals,
                height = dimensions.buttonHeight,
                onClick = onEqualsClick
            )
        }
    }
}

enum class CalculatorButtonType {
    Numeric,
    Function,
    Operator,
    Equals
}

@Composable
fun CalculatorButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    buttonType: CalculatorButtonType = CalculatorButtonType.Numeric,
    height: Dp = 74.dp,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "btn_press_scale"
    )

    val shape = RoundedCornerShape(percent = 50)

    val colors = when (buttonType) {
        CalculatorButtonType.Function -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        CalculatorButtonType.Function -> MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (height.value * 0.32).sp,
            letterSpacing = 0.2.sp
        )
        CalculatorButtonType.Operator -> MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (height.value * 0.44).sp
        )
        CalculatorButtonType.Equals -> MaterialTheme.typography.headlineLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (height.value * 0.46).sp
        )
        CalculatorButtonType.Numeric -> MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (height.value * 0.38).sp
        )
    }

    Button(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { this.contentDescription = contentDescription },
        shape = shape,
        colors = colors,
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center
        )
    }
}
