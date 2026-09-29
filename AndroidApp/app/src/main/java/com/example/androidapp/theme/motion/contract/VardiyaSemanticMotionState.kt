package com.example.androidapp.theme.motion.contract

/**
 * Semantic interaction and lifecycle motion states for Vardiya UI components.
 *
 * ============================================================================
 * CRITICAL SOURCE-OF-TRUTH SEPARATION:
 * ============================================================================
 *
 * UPSTREAM MATERIAL EXPRESSIVE PRINCIPLE:
 * Material 3 Expressive motion binds spring dynamics to user intent and component
 * lifecycle states rather than arbitrary duration curves. Transitions are categorized
 * as spatial (shape, bounds, position) or effects (color, opacity).
 *
 * VARDIYA IMPLEMENTATION DECISION:
 * We map shift tracking domain states and Compose UI interaction states into these
 * six discrete semantic states. Each state corresponds to justified Vardiya UI needs:
 * - RESTING: Default idle state for buttons, cards, cells, indicators, and hero.
 * - PRESSED: Direct touch down on interactive controls (ControlBar buttons, calendar cells).
 * - SELECTED: Selected toggle state (calendar day cell, analytics period filter chip).
 * - ACTIVE: Active domain execution (running shift in hero, active pulsing badge).
 * - EXPANDED: Morph into open container configuration (settings accordion).
 * - DISABLED: Non-interactive or suppressed state.
 */
enum class VardiyaSemanticMotionState {
    /**
     * Default idle state awaiting user interaction or lifecycle activation.
     */
    RESTING,

    /**
     * Component is currently undergoing direct touch/pointer depression.
     */
    PRESSED,

    /**
     * Component represents an actively chosen option (e.g. active tab, selected calendar cell).
     */
    SELECTED,

    /**
     * Component is actively executing its core continuous domain behavior (e.g. active shift running).
     */
    ACTIVE,

    /**
     * Component container has morphed into its open/revealed configuration (e.g. settings accordion).
     */
    EXPANDED,

    /**
     * Component is non-interactive or suppressed.
     */
    DISABLED
}
