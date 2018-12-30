package io.github.yangziwen.quickdao.core;

import io.github.yangziwen.quickdao.core.util.VarArgsSQLFunction;

public enum Operator {

    eq {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " = " + placeholder;
        }
    },

    ne {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " != " + placeholder;
        }
    },

    gt {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " > " + placeholder;
        }
    },

    ge {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " >= " + placeholder;
        }
    },

    lt {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " < " + placeholder;
        }
    },

    le {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " <= " + placeholder;
        }
    },

    contain {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " LIKE " + chooseConcatFunc().render("'%'", placeholder, "'%'");
        }
    },

    not_contain {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " NOT LIKE " + chooseConcatFunc().render("'%'", placeholder, "'%'");
        }
    },

    start_with {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " LIKE " + chooseConcatFunc().render(placeholder, "'%'");
        }
    },

    not_start_with {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " NOT LIKE " + chooseConcatFunc().render(placeholder, "'%'");
        }
    },

    end_with {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " LIKE " + chooseConcatFunc().render("'%'", placeholder);
        }
    },

    not_end_with {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " NOT LIKE " + chooseConcatFunc().render("'%'", placeholder);
        }
    },

    in {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " IN (" + placeholder + ")" ;
        }
    },

    not_in {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " NOT IN (" + placeholder + ")";
        }
    },

    is_null {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " IS NULL ";
        }
    },

    is_not_null {
        @Override
        public String buildCondition(String stmt, String placeholder) {
            return stmt + " IS NOT NULL";
        }
    };

    private static final VarArgsSQLFunction MYSQL_CONCAT_FUNC = new VarArgsSQLFunction("CONCAT(", ", ", ")");

    private static final VarArgsSQLFunction SQLITE_CONCAT_FUNC = new VarArgsSQLFunction("", "||", "");

    public abstract String buildCondition(String stmt, String placeholder);

    private static VarArgsSQLFunction chooseConcatFunc() {
        return MYSQL_CONCAT_FUNC;
    }

}
