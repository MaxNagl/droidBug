package de.siebn.javaBug;

public enum BugFormat {
    colorNeutral,
    colorNeutralLight,
    colorPrimary,
    colorPrimaryLight,
    colorSecondary,
    colorSecondaryLight,
    colorTernary,
    colorTernaryLight,
    colorError,

    title(colorPrimary),
    category(colorSecondary),
    modifier(colorPrimary),
    file(colorPrimary),
    clazz(colorPrimary),
    method(colorPrimaryLight),
    field(colorPrimaryLight),
    value(colorTernary),
    nul(colorTernaryLight),

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
