package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.HeroStyleImageGridLayoutDimensions.GRID_CELLS
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.HeroStyleImageGridLayoutDimensions.contentPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.HeroStyleImageGridLayoutDimensions.gridCellSpacing
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.HeroStyleImageGridLayoutDimensions.itemWidth
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity

/**
 * A layout focused on presenting a grid of images (with optional title and supporting text) below
 * a hero image.
 *
 * An app-specific [TitleBar] is displayed over the hero image. Since it is displayed over the
 * hero image, title text is not displayed. The action icon buttons in the title bar are displayed
 * with a solid background color for visibility.
 *
 * [com.example.platform.ui.appwidgets.glance.layout.collections.HeroStyleImageGridAppWidget] loads the images as bitmaps,
 * and scales them to fit into widget's limits. Number of items are capped. When using bitmaps,
 * this approach is suitable for use cases that recommend / feature limited items.
 *
 * The image grid below the hero image is always shown with two columns. The layout is suitable
 * for images with 16:9 aspect ratio. The layout serves as an implementation suggestion, but
 * should be customized to fit your product's needs. For example, you may use a 16:9 aspect ratio
 * image for the hero (for its wide display), and use other aspect ratio images for the grid.
 *
 * @param titleIconRes a tintable icon that represents your app or brand. In this sample, we use
 * icon from a drawable resource, but you should use an appropriate icon source for your use case.
 * @param titleBarActionIconRes resource id of a tintable icon that can be displayed as
 * an icon button within the title bar area of the widget. For example, a search icon.
 * @param titleBarActionIconContentDescription description of the [titleBarActionIconRes] button
 * to be used by the accessibility services.
 * @param titleBarAction action to be performed on click of the [titleBarActionIconRes] button.
 * @param items list of items to be displayed in the grid; typically, image with optional title
 *              and supporting text.
 *
 * @see [ImageGridItemData] for accepted inputs.
 * @see [com.example.platform.ui.appwidgets.glance.layout.collections.ImageGridAppWidget]
 */
@Composable
fun HeroStyleImageGridLayout(
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int,
  titleBarActionIconContentDescription: String,
  titleBarAction: () -> Unit,
  items: List<ImageGridItemData>,
) {
  @Composable
  fun TitleBar(filledActionButtons: Boolean = false) {
    val actionButtonBackgroundColor =
      GlanceTheme.colors.inverseOnSurface.takeIf { filledActionButtons }

    TitleBar(
      startIcon = ImageProvider(titleIconRes),
      title = "",
      iconColor = GlanceTheme.colors.primary,
      textColor = GlanceTheme.colors.onSurface,
      actions = {
        CircleIconButton(
          imageProvider = ImageProvider(titleBarActionIconRes),
          contentDescription = titleBarActionIconContentDescription,
          contentColor = GlanceTheme.colors.secondary,
          backgroundColor = actionButtonBackgroundColor,
          onClick = titleBarAction
        )
      }
    )
  }

  @Composable
  fun EmptyListWidgetContent() {
    Scaffold(
      backgroundColor = GlanceTheme.colors.widgetBackground,
      titleBar = {
        TitleBar(filledActionButtons = false)
      }
    ) {
      EmptyListContent()
    }
  }

  @Composable
  fun HeroGridWidgetContent() {
      val isApi31 = android.os.Build.VERSION.SDK_INT >= 31
      val cornerRadiusModifier = if (isApi31) {

          val systemCornerRadiusDefined = LocalContext.current.resources
              .getResourceName(android.R.dimen.system_app_widget_background_radius) != null

          if ( systemCornerRadiusDefined) {
                  GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
          } else {
              GlanceModifier
          }
      } else {
          GlanceModifier
      }


    Box(
      GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(GlanceTheme.colors.widgetBackground)
        .then(cornerRadiusModifier)
    ) {
      ScrollableHeroGrid(
        items = items,
        titleBar = { TitleBar(filledActionButtons = true) }
      )
    }
  }

  if (items.isEmpty()) {
    EmptyListWidgetContent()
  } else {
    HeroGridWidgetContent()
  }
}

/**
 * Displays a scrollable view with hero image, a title-bar over the hero image, and a grid of
 * images below them.
 */
@Composable
private fun ScrollableHeroGrid(items: List<ImageGridItemData>, titleBar: @Composable () -> Unit) {
  val heroItem = items.first()
  val gridItems = items.subList(1, items.size) // without hero item
  var gridItemIndex = 0

  @Composable
  fun HeroWithTitleBarOverlay() {
    Box(
      modifier = GlanceModifier
        .fillMaxWidth()
        .padding(bottom = gridCellSpacing)
    ) {
      GridItem(
        item = heroItem,
        modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
        textStartMargin = contentPadding // Align the text of hero item with grid below
      )
      Box(
        // Align the title-bar with grid content below
        modifier = GlanceModifier.padding(contentPadding)
      ) {
        titleBar()
      }
    }
  }

  /**
   * A pseudo grid row used within a LazyColumn to mimic a grid.
   */
  @Composable
  fun GridRow(isLastRow: Boolean) {
    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .padding(horizontal = contentPadding)
        .padding(bottom = gridCellSpacing.takeIf { isLastRow } ?: 0.dp)
    ) {
      // item 1
      GridItem(
        item = gridItems[gridItemIndex++],
        modifier = GlanceModifier
          .width(itemWidth)
          .padding(end = gridCellSpacing)
          .defaultWeight()
      )
      if (gridItemIndex < gridItems.size) {
        // item 2
        GridItem(
          item = gridItems[gridItemIndex++],
          modifier = GlanceModifier
            .width(itemWidth)
        )
      }
    }
  }

  LazyColumn(
    modifier = GlanceModifier.semantics {
      // For a movies widget, this could be: "Top 7 Movies for you". Since we use a LazyColumn to
      // implement a pseudo grid, this acts as a cue for user to know how many items are
      // available.
      contentDescription = "Top ${items.size} items for you"
    }
  ) {
    val numberOfRows = gridItems.size / GRID_CELLS

    item {
      // Note: The title bar is overlay over the hero image, so, the current order of traversal
      // with the screen readers is - hero image, title bar action buttons, then the non-hero
      // items.
      // TODO: Explore if we can have the titlebar to be read first before Hero image.
      // e.g. RemoteViews have setAccessibilityTraversalAfter, if we could use it OR restructure
      // to get the title bar first.
      HeroWithTitleBarOverlay()
    }
    items(numberOfRows) { rowIndex ->
      // Since the wrapping row is not clickable, when navigating, screen readers will tab over to
      // each image grid item instead of navigating per lazy column item (i.e. row).
      GridRow(isLastRow = rowIndex != numberOfRows - 1)
    }
  }
}

@Composable
private fun GridItem(
  item: ImageGridItemData,
  modifier: GlanceModifier,
  textStartMargin: Dp = 4.dp,
) {
  val finalModifier =
    modifier.clickable(
      actionStartDemoActivity("Grid item: ${item.title ?: item.imageContentDescription}")
    )
  val imageProvider = if (item.image != null) {
    ImageProvider(item.image)
  } else {
    ImageProvider(R.drawable.sample_placeholder_image)
  }


  @Composable
  fun Image() {
    Image(
      provider = imageProvider,
      contentDescription = item.imageContentDescription,
      contentScale = ContentScale.Fit,
      modifier = GlanceModifier
        .fillMaxWidth()
        .wrapContentHeight()
        .cornerRadius(16.dp)
    )
  }

  @Composable
  fun Title(text: String) {
    Text(
      text = text,
      maxLines = 1,
      style = HeroStyleImageGridLayoutTextStyles.titleText,
      modifier = GlanceModifier.padding(start = textStartMargin)
    )
  }

  @Composable
  fun SupportingText(text: String) {
    Text(
      text = text,
      maxLines = 1,
      style = HeroStyleImageGridLayoutTextStyles.supportingText,
      modifier = GlanceModifier.padding(start = textStartMargin)
    )
  }

  if (item.title != null) {
    VerticalListItem(
      modifier = finalModifier,
      topContent = { Image() },
      titleContent = { Title(text = item.title) },
      supportingContent = takeComposableIf(item.supportingText != null) {
        SupportingText(text = checkNotNull(item.supportingText))
      }
    )
  } else {
    Box(finalModifier) {
      Image()
    }
  }
}

/** Returns the provided [block] composable if [predicate] is true, else returns null */
@Composable
private inline fun takeComposableIf(
  predicate: Boolean,
  crossinline block: @Composable () -> Unit,
): (@Composable () -> Unit)? {
  return if (predicate) {
    { block() }
  } else null
}

private object HeroStyleImageGridLayoutTextStyles {
  /**
   * Style for the text displayed as title within each item.
   */
  val titleText: TextStyle
    @Composable get() = TextStyle(
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp, // M3 Title Medium
      color = GlanceTheme.colors.onSurface
    )

  /**
   * Style for the text displayed as supporting text within each item.
   */
  val supportingText: TextStyle
    @Composable get() =
      TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, // M3 Label Medium
        color = GlanceTheme.colors.secondary
      )
}

private object HeroStyleImageGridLayoutDimensions {
  /**
   * Padding around the content (grid section).
   */
  val contentPadding: Dp = 12.dp

  /**
   * Space between each cell in the grid
   */
  val gridCellSpacing: Dp = 12.dp

  /**
   * Number of columns in the bottom grid
   */
  const val GRID_CELLS = 2

  /**
   * Width available for each item's content within the grid
   */
  val itemWidth: Dp
    @Composable get() {
      // exclude horizontal paddings
      val totalContentWidth = LocalSize.current.width - (contentPadding * 2)
      val totalHorizontalCellSpacing = gridCellSpacing * (GRID_CELLS - 1)
      return (totalContentWidth - totalHorizontalCellSpacing) / GRID_CELLS
    }
}