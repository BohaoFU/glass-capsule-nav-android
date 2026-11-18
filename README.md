# Glass Capsule Nav for Android

A floating navigation bar for Android Views. It blurs scrolling content behind it and slides a fixed-width selection capsule between equally spaced tabs. Built-in icons scale and transition between outline and translucent fill.

<img src="docs/demo.png" width="360" alt="Glass Capsule demo with the Notifications tab selected over a blurred color panel">

## Run the demo

Use JDK 17 and Android SDK 36. The library and demo target devices running Android 11 (API 30) or later.

```sh
./gradlew :demo:installDebug
```

Open **Glass Capsule**, scroll the color panels, and switch tabs. The middle **Add** button is an independent action. To build the library AAR locally, run `./gradlew :library:assembleRelease`.

## Add it to an Android Views project

Include the [library module](library/) and add `implementation project(":library")` in the app module. The [sample Gradle settings](settings.gradle) show the repositories required to resolve BlurView.

Place the bar above a view group that draws the page content. Keep them as siblings in the same window. Allow their parent to draw outside child bounds with `android:clipChildren="false"` so the shadow can spread. Inside `GlassNavBar`, put the tabs in a horizontal `LinearLayout` with ID `@id/glass_nav_tabs` and use the included styles. See the [complete XML layout](demo/src/main/res/layout/activity_demo.xml).

```java
GlassNavBar bar = findViewById(R.id.glass_bar);
GlassNavTab home = findViewById(R.id.tab_home);
GlassNavTab notifications = findViewById(R.id.tab_notifications);

bar.setBackdrop(findViewById(R.id.backdrop), null);
bar.selectTab(home, false);
notifications.setOnClickListener(view -> {
    // Switch the page in your own navigation code.
    bar.selectTab(notifications, true);
});
```

- `glassNavLabel` sets the visible label and accessible name. `glassNavIcon` accepts `home`, `message`, `add`, `notification`, or `profile`.
- Use `tab.getIconImageView()` to supply a custom drawable or a circle-cropped avatar. Clearing the drawable restores the built-in icon.
- Set click handlers on each tab. For standalone actions such as **Add**, keep the current selection by leaving `selectTab` out of that action's handler.
- Light and dark palettes follow Android's resource configuration. Selection animation respects the system animator setting.

## 中文说明

这是一个原生 Android Views 玻璃底栏组件，包含实时背景模糊、固定宽度的滑动胶囊、图标缩放与镂空填充，以及头像替换。示例与组件分开，运行 `./gradlew :demo:installDebug` 后可在手机上滚动背景、切换选项并体验中央独立按钮。

## License

Project code is available under the [MIT License](LICENSE). The [Lucide icon notices](library/src/main/assets/licenses/lucide.txt) are bundled with the library. The live blur uses [BlurView](https://github.com/Dimezis/BlurView/tree/version-2.0.6), distributed under [Apache 2.0](https://github.com/Dimezis/BlurView/blob/version-2.0.6/LICENSE.md).
