# Implementation Plan - UI Consistency: High-Density Card Redesign

This plan ensures visual consistency and further reduces the height of all cards across the Dashboard, History, and Cards tabs.

## User Review Required

> [!NOTE]
> All cards will now follow a unified, high-density layout to maximize information visibility on one screen.

## Proposed Changes

### Dashboard & History Tabs

#### [MODIFY] [Components.kt](file:///Users/igeneration/AndroidStudioProjects/CardBill/app/src/main/java/com/finsignal/ui/components/Components.kt)
- **SummaryCard Redesign**:
    - Reduce padding from `24.dp` to `16.dp`.
    - Compact the vertical spacing between "Total Due" and "Previous Balance".
    - Reduce title typography sizes.
- **BillCard Further Compactness**:
    - Reduce vertical padding from `12.dp` to `8.dp`.
    - Adjust internal `Spacer` heights to `4.dp` or `6.dp`.
    - Ensure "Due Date" remains **Red** (`StatusOverdue`) for consistency.

### Navigation Consistency

#### [MODIFY] [DashboardScreen.kt](file:///Users/igeneration/AndroidStudioProjects/CardBill/app/src/main/java/com/finsignal/ui/dashboard/DashboardScreen.kt)
- Update `verticalArrangement` in `LazyColumn` to use a tighter spacing (`8.dp` instead of `12.dp`).

#### [MODIFY] [HistoryScreen.kt](file:///Users/igeneration/AndroidStudioProjects/CardBill/app/src/main/java/com/finsignal/ui/history/HistoryScreen.kt)
- Update `verticalArrangement` in `LazyColumn` to use the same tight spacing (`8.dp`).

### Cards Tab

#### [MODIFY] [CardsScreen.kt](file:///Users/igeneration/AndroidStudioProjects/CardBill/app/src/main/java/com/finsignal/ui/cards/CardsScreen.kt)
- Update `verticalArrangement` in `LazyColumn` to `8.dp`.
- Ensure `CardItem` padding and corner radius matches the new global high-density style.

## Verification Plan

### Manual Verification
- **All Tabs**: Verify that the vertical gap between cards is reduced and consistent (`8.dp`).
- **Dashboard**: Verify the `SummaryCard` is more compact.
- **History/Dashboard**: Verify `BillCard` height is further reduced.
- **Cards Tab**: Verify `CardItem` height matches the `BillCard` aesthetic.
