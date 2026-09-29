# Material 3 Expressive Source Map & Upstream Reference Audit

> **Target Project:** Vardiya (Shift & Wage Tracking for Android)  
> **Source Repository:** Google Material Components for Android (`material-components-android`)  
> **Documentation Type:** Upstream Implementation Audit & Source Mapping  
> **Date:** September 2026  

---

## 1. Upstream Repository Pin & Source Metadata

To ensure deterministic, reproducible engineering and avoid relying on moving HEADs or marketing prose, the upstream source reference is pinned as follows:

| Field | Pinned Source Specification |
| :--- | :--- |
| **Repository URL** | `https://github.com/material-components/material-components-android` |
| **Local Reference Location** | `reference/material-components-android/` |
| **Branch** | `master` (Tracked post-1.14.0 release) |
| **Pinned Commit SHA** | `60ff09436d5d477a4b9d02940f31eb01e1250620` |
| **Source Revision Date** | `2026-09-11 14:22:27 +0000` (`PiperOrigin-RevId: 979807273`) |
| **Latest Stable Tag Reference** | `1.14.0` (`commit 66c334b7946dabf33adfe1a2b7cad6bcaa4ea3ad`) |
| **Dependency Status** | **0% Runtime Dependency** (Reference & architectural guidance only) |

---

## 2. Executive Source Architecture Summary

Google's Material 3 Expressive system in `material-components-android` is defined by four core architectural layers:

1. **Expressive Design Tokens (`tokens.xml`):**  
   Defines numerical spring parameters partitioned into two distinct physical domains:
   - **Spatial Springs:** Low damping ratio (\(0.6 - 0.8\)) allowing organic, perceptible overshoot/settling for spatial movement, shape morphing, and container scaling.
   - **Effects Springs:** Strictly critically damped (\(1.0\)) with high stiffness (\(800 - 3800\)) preventing unnatural oscillation in opacity, color, or elevation transitions.
2. **Dynamic Corner Spring Architecture (`MaterialShapeDrawable.java`):**  
   Components do not rely on static shapes. Four independent `SpringAnimation` instances drive the four corner radii using `setCornerSpringForce()`, bound to `StateListShapeAppearanceModel`.
3. **Connected Shape & Width Morphing (`MaterialButtonGroup.java`):**  
   Buttons in a group share bounding physics; when an item expands or is selected, its neighbors dynamically absorb the width delta (`onButtonWidthChanged`), while inner corners are flattened to form an organic continuous container.
4. **Wavy Indicator Geometry (`CircularDrawingDelegate.java` / `LinearDrawingDelegate.java`):**  
   Wavy indicators use cubic Bezier cycles anchored to the circular track. Crucially, the **inactive track is a smooth, continuous circle**, while the **active indicator carries the traveling sinusoidal wave**.

---

## 3. Comprehensive Component Source Mapping Table

| Material Component | Upstream Source File | Expressive Behavior & Geometry | Motion Primitive | Vardiya Equivalent Surface | Integration Priority |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Circular Wavy Progress Indicator** | `lib/.../progressindicator/CircularDrawingDelegate.java`<br>`CircularProgressIndicatorSpec.java` | 12-segment cubic Bezier traveling wave, smooth circular track with round caps, 4dp air gap, dynamic wave amplitude and phase accumulator. | Custom Canvas Bezier math + Spatial Spring for amplitude/progress + Effects Spring for color. | `CircularWavyProgressHero.kt` (Hero shift earnings ring) | **P0 (Hero)** |
| **Linear Wavy Progress Indicator** | `lib/.../progressindicator/LinearDrawingDelegate.java` | Continuous sinusoidal traveling wave with cycle count determined by wavelength; smooth inactive track. | Custom Canvas + Spatial Spring for wave amplitude. | Micro-indicators (syncing, backup progress) | **P3 (Secondary)** |
| **Expressive Floating Toolbar** | `lib/.../floatingtoolbar/FloatingToolbarLayout.java`<br>`res/values/styles.xml` | Floating pill-shaped container hosting tonal icon buttons and action buttons; elevation 4dp, rounded corners 32dp. | Container Transform + Spatial Spring size adjustment. | `VardiyaControlBar.kt` (Floating shift action bar) | **P0 (Hero Control)** |
| **Button Group Connected Morph** | `lib/.../button/MaterialButtonGroup.java`<br>`MaterialSplitButton.java` | Inner corners collapse to 0 or small radius (`innerCornerSize`); width change on active item causes neighbors to absorb width delta (`onButtonWidthChanged`). | Spatial Spring (`motionSpringFastSpatial`, damping 0.6, stiffness 800). | `VardiyaControlBar.kt` (Play/Pause/Break/Finish pill layout) | **P1 (Primary)** |
| **Corner Radius State Morphing** | `lib/.../shape/MaterialShapeDrawable.java`<br>`StateListShapeAppearanceModel.java` | Each corner has an independent `SpringAnimation` that morphs radius on state changes (pressed, checked, selected). | Spatial Spring (`cornerSpringAnimations` using `CORNER_SIZES_IN_PX`). | `CalendarHeatmap.kt` (Day cell selection morph 10dp → 14dp) & `StateBadge` | **P1 (Primary)** |
| **Shared Axis Screen Transition** | `lib/.../transition/SlideDistanceProvider.java`<br>`MaterialSharedAxis.java` | Directional sliding over a subtle 30dp distance (~15–22% screen width) combined with crossfade, avoiding full-screen sweeps. | Spatial Spring slide + Effects Spring fade. | `VardiyaNavTransitions` in `VardiyaAppScaffold.kt` | **P1 (Primary)** |
| **Search View Container Transform** | `lib/.../search/SearchViewAnimationHelper.java` | SearchBar expands into full-screen SearchView using spring scaling and corner radius expansion. | Spatial Spring (`getExpandCollapseSpringAnimations`). | Settings search & history filter dialogs | **P2 (Supporting)** |
| **Predictive Back Scale** | `lib/.../motion/MaterialMainContainerBackHelper.java` | Container scales down smoothly to 0.90f (`MIN_SCALE`) with edge margins and rounded corner expansion during back gesture. | Spring / ValueAnimator tracking gesture progress. | Navigation 3 Predictive Back handler | **P2 (Supporting)** |
| **Bottom Navigation Active Indicator** | `lib/.../bottomnavigation/res/values/styles.xml`<br>`NavigationBarItemView.java` | Active indicator morphs in width and height (`activeIndicatorResizeable = true`), adapting between compact and expanded states. | Spatial Spring size morph + Effects Spring alpha. | `VardiyaNavigationBar` & `VardiyaNavigationRail` | **P2 (Supporting)** |
| **Swipe-to-Reveal Actions** | `lib/.../listitem/ListItemRevealLayout.java` | Swiped list item reveals actions whose widths scale proportionally based on intrinsic ratios. | Dynamic proportional layout measurement. | Shift history list item swipe delete/edit | **P3 (Future)** |
| **Analytics Charts** | *None in upstream library* | Google does not provide chart components in Material Components; charts are domain-specific. | Micro-motion (spring scale on bar selection) borrowed from `MaterialCardView`. | `AnalyticsCharts.kt` & `VardiyaAnalyticsScreen.kt` | **P2 (Domain Custom)** |

---

## 4. Deep-Dive Component Technical Analysis

### A. Circular Wavy Progress Hero Ring
- **Google Implementation:**  
  In `CircularDrawingDelegate.java`, Google builds the circular wavy indicator using cubic Bezier curves (`appendCubicPerHalfCycle`). The wave frequency is computed from `basePathLength / wavelength / 2`. The inactive track is rendered via `fillTrack` with `amplitudeFraction = 0f`, ensuring the inactive background remains a clean, smooth circle while the active progress arm carries the animated wave.
- **Trigger:**  
  Continuous frame clock advances `phaseFraction` while active; progress changes advance `startFraction` / `endFraction`.
- **Motion Primitives:**  
  Spatial spring for progress and amplitude; effects spring for color; frame clock for wave phase.
- **Geometry Changes:**  
  Amplitude modulates with state; traveling phase creates sinusoidal oscillation; corner caps ramp down at 99% progress.
- **Vardiya Mapping:**  
  Authoritative model for `CircularWavyProgressHero.kt`. The hero ring uses 12 waves around 360°, a smooth non-overlapping track, and dynamic amplitude levels across shift lifecycle states (`NOT_STARTED`: 2.5dp, `PAUSED`: 3.5dp, `BREAK`: 4.0dp, `FINISHED`: 5.0dp, `RUNNING`: 6.5dp, `OVERTIME`: 8.5dp) paired with container spring scaling (0.96f to 1.02f).

### B. Floating Action Bar & Control Bar
- **Google Implementation:**  
  In `FloatingToolbarLayout.java` and `MaterialButtonGroup.java`, Google wraps action buttons in a floating, elevated surface (elevation 4dp, corner radius 32dp / pill). In `MaterialButtonGroup`, inner corners between adjacent buttons are flattened (`innerCornerSize = 0`), creating a unified shape. When button dimensions change, `onButtonWidthChanged` coordinates neighbors to absorb width deltas.
- **Trigger:**  
  User tap, shift state transition (Start, Pause, Resume, Break, Finish).
- **Motion Primitives:**  
  Spatial spring (`motionSpringFastSpatial`, damping 0.6, stiffness 800) for container resizing and button scale/slide.
- **Geometry Changes:**  
  Container morphs width and height; button scale transitions (0.88f enter/exit); vertical directional slide (`±height/5`); unclipped container expansion (`SizeTransform(clip = false)`).
- **Vardiya Mapping:**  
  Directly implemented in `VardiyaControlBar.kt`. Eliminates abrupt text crossfades by allowing the control pill to morph organically across its 5 semantic layout configurations (`START_ONLY`, `ACTIVE_CONTROLS`, `BREAK_CONTROLS`, `PAUSED_CONTROLS`, `RESET_ONLY`).

### C. Navigation & Screen Transitions
- **Google Implementation:**  
  In `SlideDistanceProvider.java` and `MaterialSharedAxis.java`, Google rejects full-screen (100% width) slide carousels for top-level navigation. Instead, it defines `R.dimen.mtrl_transition_shared_axis_slide_distance = 30dp`. On mobile screens, 30dp translates to approximately 15%–22% of total screen width.
- **Trigger:**  
  Navigation event between top-level tabs (Home, Analytics, History, Settings).
- **Motion Primitives:**  
  Expressive spatial spring (`defaultSpatialSpec`) for horizontal translation; critically damped effects spring (`defaultEffectsSpec`) for fade.
- **Geometry Changes:**  
  Entering screen slides in from `+22%` width and fades in; exiting screen slides out to `-22%` width and fades out.
- **Vardiya Mapping:**  
  Implemented in `VardiyaNavTransitions` (`SubtleSpatialOffsetFactor = 0.22f`). Under `MotionPreference.REDUCED`, the spatial offset collapses to 0dp (pure effects fade).

### D. Settings Expandable Surfaces
- **Google Implementation:**  
  `MaterialContainerTransform` coordinates the expansion of a compact surface into a detailed view. Rather than dropping down like a rigid curtain, the container scales from top to bottom while internal content slides slightly into place.
- **Trigger:**  
  User expands an accordion card (Salary configuration, Night rate details).
- **Motion Primitives:**  
  Spatial spring (`defaultSpatialSpec`) for vertical expansion + vertical slide; effects spring (`defaultEffectsSpec`) for opacity.
- **Geometry Changes:**  
  `expandVertically(expandFrom = Top) + slideInVertically(-height/4) + fadeIn()`.
- **Vardiya Mapping:**  
  Implemented in `VardiyaExpandableMotion.kt`. Gives accordion items physical weight and inertia when expanding or collapsing.

### E. Analytics Charts (Custom Domain Surface)
- **Google Implementation:**  
  **No equivalent component exists in Material Components.** Charts are domain-specific data visualizations.
- **Upstream Principle:**  
  Borrow the selection interaction principle from `MaterialCardView` and `MaterialButton`: when an item is selected, it must not merely change color; it should respond with a subtle spatial scale "pop" (1.08f) and shape refinement.
- **Vardiya Mapping:**  
  - Period switching (Weekly ↔ Monthly) uses directional horizontal slide (15% offset) + scale morph (0.95f).
  - Individual bar taps animate `barScale` to `1.08f` via `graphicsLayer` using `fastSpatialSpec()`.

### F. Calendar Heatmap
- **Google Implementation:**  
  `CalendarItemStyle.java` and `MaterialDatePicker.java` manage calendar day cells. In Google's design system, active/selected cells transition their selection marker using shape and elevation.
- **Upstream Principle:**  
  Applying `MaterialShapeDrawable` corner spring animation (`cornerSpringAnimations`): upon selection, a cell morphs its shape (e.g. from rounded rectangle 10dp to pill/soft shape 14dp) while scaling up (1.08f).
- **Vardiya Mapping:**  
  Implemented in `CalendarDayCell` inside `CalendarHeatmap.kt`. Tapping a day smoothly springs its corner radius from 10dp to 14dp and scales to 1.08f.

### G. StateBadge & Micro Indicators
- **Google Implementation:**  
  `BadgeDrawable.java` is a static badge without continuous pulse animations. Continuous breathing in Material Design is reserved for live progress or active states.
- **Upstream Principle:**  
  When an indicator pulses, pure alpha breathing looks flat. Combining alpha breathing (0.35 to 1.0) with physical scale breathing (0.88f to 1.15f) communicates vitality. Crucially, **zero frame loops or CPU work must occur when idle** (NOT_STARTED, PAUSED, on BREAK, or FINISHED).
- **Vardiya Mapping:**  
  Implemented in `StateBadge` inside `VardiyaScreen.kt`, driven by `StateBadgeMotion`.
