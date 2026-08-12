/* SPDX-License-Identifier: MIT */

package cn.tealc.wutheringwavestool.theme;

import atlantafx.base.theme.Theme;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class ThemeManager {
    public static final String DEFAULT_FONT_FAMILY_NAME = "Inter";
    public static final int DEFAULT_FONT_SIZE = 14;
    public static final int DEFAULT_ZOOM = 100;
    public static final AccentColor DEFAULT_ACCENT_COLOR = null;
    public static final List<Integer> SUPPORTED_FONT_SIZE = IntStream.range(8, 29).boxed().collect(Collectors.toList());
    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass USER_CUSTOM = PseudoClass.getPseudoClass("user-custom");
    private final Map<String, String> customCSSDeclarations = new LinkedHashMap<>(); // -fx-property | value;
    private final Map<String, String> customCSSRules = new LinkedHashMap<>(); // .foo | -fx-property: value;
    private Scene scene;
    private String fontFamily = DEFAULT_FONT_FAMILY_NAME;
    private int fontSize = DEFAULT_FONT_SIZE;

    private Theme currentTheme;

    private AccentColor accentColor = DEFAULT_ACCENT_COLOR;

    public Theme getTheme() {
        return currentTheme;
    }


    public void setTheme(Theme theme) {
        Objects.requireNonNull(theme);

        if (currentTheme != null) {
            animateThemeChange(Duration.millis(750));
        }

        Application.setUserAgentStylesheet(Objects.requireNonNull(theme.getUserAgentStylesheet()));
        //getScene().getStylesheets().setAll(theme.getAllStylesheets());
        getScene().getRoot().pseudoClassStateChanged(DARK, theme.isDarkMode());

        // remove user CSS customizations and reset accent on theme change
        resetAccentColor();
        resetCustomCSS();

        currentTheme = theme;
        //EVENT_BUS.publish(new ThemeEvent(EventType.THEME_CHANGE));
    }


    public void resetAccentColor() {
        animateThemeChange(Duration.millis(350));

        if (accentColor != null) {
            getScene().getRoot().pseudoClassStateChanged(accentColor.pseudoClass(), false);
            accentColor = null;
        }

        //EVENT_BUS.publish(new ThemeEvent(EventType.COLOR_CHANGE));
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        Objects.requireNonNull(fontFamily);
        setCustomDeclaration("-fx-font-family", "\"" + fontFamily + "\"");
        this.fontFamily = fontFamily;
        reloadCustomCSS();
        //EVENT_BUS.publish(new ThemeEvent(EventType.FONT_CHANGE));
    }

    public boolean isDefaultFontFamily() {
        return Objects.equals(DEFAULT_FONT_FAMILY_NAME, getFontFamily());
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int size) {
        if (!SUPPORTED_FONT_SIZE.contains(size)) {
            throw new IllegalArgumentException(
                    String.format("Font size must in the range %d-%dpx. Actual value is %d.",
                            SUPPORTED_FONT_SIZE.get(0),
                            SUPPORTED_FONT_SIZE.get(SUPPORTED_FONT_SIZE.size() - 1),
                            size
                    ));
        }

        setCustomDeclaration("-fx-font-size", size + "px");
        setCustomRule(".ikonli-font-icon", String.format("-fx-icon-size: %dpx;", size + 2));

        this.fontSize = size;


        reloadCustomCSS();
        // EVENT_BUS.publish(new ThemeEvent(EventType.FONT_CHANGE));
    }

    public boolean isDefaultSize() {
        return DEFAULT_FONT_SIZE == fontSize;
    }

    private void animateThemeChange(Duration duration) {
        Image snapshot = scene.snapshot(null);
        Pane root = (Pane) scene.getRoot();

        ImageView imageView = new ImageView(snapshot);
        root.getChildren().add(imageView); // add snapshot on top

        var transition = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_OUT)),
                new KeyFrame(duration, new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT))
        );
        transition.setOnFinished(e -> root.getChildren().remove(imageView));
        transition.play();
    }

    private void setCustomRule(String selector, String rule) {
        customCSSRules.put(selector, rule);
    }

    private void setCustomDeclaration(String property, String value) {
        customCSSDeclarations.put(property, value);
    }

    private void reloadCustomCSS() {
        Objects.requireNonNull(scene);
        StringBuilder css = new StringBuilder();

        css.append(".root:");
        css.append(USER_CUSTOM.getPseudoClassName());
        css.append(" {\n");
        customCSSDeclarations.forEach((k, v) -> {
            css.append("\t");
            css.append(k);
            css.append(":\s");
            css.append(v);
            css.append(";\n");
        });
        css.append("}\n");

        customCSSRules.forEach((k, v) -> {
            // custom CSS is applied to the body,
            // thus it has a preference over accent color
            css.append(".body:");
            css.append(USER_CUSTOM.getPseudoClassName());
            css.append(" ");
            css.append(k);
            css.append(" {");
            css.append(v);
            css.append("}\n");
        });

        getScene().getRoot().getStylesheets().removeIf(uri -> uri.startsWith("data:text/css"));
        getScene().getRoot().getStylesheets().add(
                "data:text/css;base64," + Base64.getEncoder().encodeToString(css.toString().getBytes(UTF_8))
        );
        getScene().getRoot().pseudoClassStateChanged(USER_CUSTOM, true);
    }

    public void resetCustomCSS() {
        customCSSDeclarations.clear();
        customCSSRules.clear();
        getScene().getRoot().pseudoClassStateChanged(USER_CUSTOM, false);
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Singleton                                                             //
    ///////////////////////////////////////////////////////////////////////////
    private ThemeManager() {
    }

    private static class InstanceHolder {

        private static final ThemeManager INSTANCE = new ThemeManager();
    }

    public static ThemeManager getInstance() {
        return InstanceHolder.INSTANCE;
    }


}
