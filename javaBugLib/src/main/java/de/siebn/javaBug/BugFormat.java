package de.siebn.javaBug;

public enum BugFormat {
    paddingNormal,

    colorNeutral,
    colorNeutralLight,
    colorPrimary,
    colorPrimaryLight,
    colorSecondary,
    colorSecondaryLight,
    colorTernary,
    colorTernaryLight,
    colorError,

    bgDark,
    bgNormal,
    bgLight,

    tabContent(paddingNormal),

    title(colorPrimary),
    category(colorSecondary),
    modifier(colorPrimary),
    file(colorPrimary),
    clazz(colorPrimary),
    method(colorPrimaryLight),
    field(colorPrimaryLight),
    value(colorTernary),
    nul(colorTernaryLight),

    button,

    autoScale,
    autoScaleCenter,
    resizeHandle,

    ;

    public final String clazzes;

    BugFormat() {
        clazzes = name();
    }

    BugFormat(BugFormat... formats) {
        StringBuilder clazzes = new StringBuilder();
        for (BugFormat format : formats) {
            if (clazzes.length() != 0) clazzes.append(" ");
            clazzes.append(format.clazzes);
        }
        this.clazzes = clazzes.toString();
    }
}
