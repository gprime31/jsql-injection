package com.jsql.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.jsql.util.tampering.TamperingType;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class TamperingUtil {

    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = Logger.getRootLogger();

    private boolean isBase64 = false;
    private boolean isVersionComment = false;
    private boolean isFunctionComment = false;
    private boolean isEqualToLike = false;
    private boolean isRandomCase = false;
    private boolean isHexToChar = false;
    private boolean isStringToChar = false;
    private boolean isQuoteToUtf8 = false;
    private boolean isEval = false;
    private boolean isSpaceToMultilineComment = false;
    private boolean isSpaceToDashComment = false;
    private boolean isSpaceToSharpComment = false;

    private String customTamper = null;

    private static final NashornScriptEngineFactory NASHORN_FACTORY = new NashornScriptEngineFactory();

    private static String eval(String sqlQuery, String jsTampering) {

        Object resultSqlTampered = null;

        try {
            if (StringUtils.isEmpty(jsTampering)) {

                throw new ScriptException("Tampering context is empty");
            }

            ScriptEngine nashornEngine = NASHORN_FACTORY.getScriptEngine();
            nashornEngine.eval(jsTampering);

            Invocable nashornInvocable = (Invocable) nashornEngine;
            resultSqlTampered = nashornInvocable.invokeFunction("tampering", sqlQuery);

        } catch (ScriptException e) {

            LOGGER.warn("Tampering context contains errors: " + e.getMessage(), e);
            resultSqlTampered = sqlQuery;

        } catch (NoSuchMethodException e) {

            LOGGER.warn("Tampering context is not properly defined: " + e.getMessage(), e);
            LOGGER.warn("Minimal tampering context is: var tampering = function(sql) {return sql}");
            resultSqlTampered = sqlQuery;
        }

        return resultSqlTampered.toString();
    }

    public String tamper(String sqlQueryDefault) {

        String lead = null;
        String sqlQuery = null;
        String trail = null;

        // Transform only SQL query without HTTP parameters and syntax changed, like
        // p=1'+[sql]
        Matcher matcherSql = Pattern.compile("(?s)(.*<tampering>)(.*)(</tampering>.*)").matcher(sqlQueryDefault);

        if (matcherSql.find()) {

            lead = matcherSql.group(1);
            sqlQuery = matcherSql.group(2);
            trail = matcherSql.group(3);

        } else {

            return sqlQueryDefault;
        }

        if (this.isEval) {

            sqlQuery = eval(sqlQuery, this.customTamper);
        }

        if (this.isHexToChar) {

            sqlQuery = eval(sqlQuery, TamperingType.HEX_TO_CHAR.instance().getJavascript());
        }

        if (this.isStringToChar) {

            sqlQuery = eval(sqlQuery, TamperingType.STRING_TO_CHAR.instance().getJavascript());
        }

        if (this.isFunctionComment) {

            sqlQuery = eval(sqlQuery, TamperingType.COMMENT_TO_METHOD_SIGNATURE.instance().getJavascript());
        }

        if (this.isVersionComment) {

            sqlQuery = eval(sqlQuery, TamperingType.VERSIONED_COMMENT_TO_METHOD_SIGNATURE.instance().getJavascript());
        }

        if (this.isRandomCase) {

            sqlQuery = eval(sqlQuery, TamperingType.RANDOM_CASE.instance().getJavascript());
        }

        if (this.isEqualToLike) {

            sqlQuery = eval(sqlQuery, TamperingType.EQUAL_TO_LIKE.instance().getJavascript());
        }

        sqlQuery = lead + sqlQuery + trail;

        sqlQuery = sqlQuery.replaceAll("(?i)<tampering>", StringUtils.EMPTY);
        sqlQuery = sqlQuery.replaceAll("(?i)</tampering>", StringUtils.EMPTY);

        // Empty when checking character insertion
        if (StringUtils.isEmpty(sqlQuery)) {

            return StringUtils.EMPTY;
        }

        // Transform all query, SQL and HTTP

        // Dependency to: EQUAL_TO_LIKE
        if (this.isSpaceToDashComment) {

            sqlQuery = eval(sqlQuery, TamperingType.SPACE_TO_DASH_COMMENT.instance().getJavascript());

        } else if (this.isSpaceToMultilineComment) {

            sqlQuery = eval(sqlQuery, TamperingType.SPACE_TO_MULTILINE_COMMENT.instance().getJavascript());

        } else if (this.isSpaceToSharpComment) {

            sqlQuery = eval(sqlQuery, TamperingType.SPACE_TO_SHARP_COMMENT.instance().getJavascript());
        }

        if (this.isBase64) {

            sqlQuery = eval(sqlQuery, TamperingType.BASE64.instance().getJavascript());
        }

        // Include character insertion at the beginning of query
        if (this.isQuoteToUtf8) {

            sqlQuery = eval(sqlQuery, TamperingType.QUOTE_TO_UTF8.instance().getJavascript());
        }

        return sqlQuery;
    }

    
    // Builder

    public TamperingUtil withBase64() {
        this.isBase64 = true;
        return this;
    }

    public TamperingUtil withVersionComment() {
        this.isVersionComment = true;
        return this;
    }

    public TamperingUtil withFunctionComment() {
        this.isFunctionComment = true;
        return this;
    }

    public TamperingUtil withEqualToLike() {
        this.isEqualToLike = true;
        return this;
    }

    public TamperingUtil withRandomCase() {
        this.isRandomCase = true;
        return this;
    }

    public TamperingUtil withHexToChar() {
        this.isHexToChar = true;
        return this;
    }

    public TamperingUtil withStringToChar() {
        this.isStringToChar = true;
        return this;
    }

    public TamperingUtil withQuoteToUtf8() {
        this.isQuoteToUtf8 = true;
        return this;
    }

    public TamperingUtil withEval() {
        this.isEval = true;
        return this;
    }

    public TamperingUtil withSpaceToMultilineComment() {
        this.isSpaceToMultilineComment = true;
        return this;
    }

    public TamperingUtil withSpaceToDashComment() {
        this.isSpaceToDashComment = true;
        return this;
    }

    public TamperingUtil withSpaceToSharpComment() {
        this.isSpaceToSharpComment = true;
        return this;
    }

    
    // Getter and setter

    public String getCustomTamper() {
        return this.customTamper;
    }

    public void setCustomTamper(String customTamper) {
        this.customTamper = customTamper;
    }

    public TamperingUtil withBase64(boolean selected) {
        this.isBase64 = selected;
        return this;
    }

    public TamperingUtil withEqualToLike(boolean selected) {
        this.isEqualToLike = selected;
        return this;
    }

    public TamperingUtil withEval(boolean selected) {
        this.isEval = selected;
        return this;
    }

    public TamperingUtil withFunctionComment(boolean selected) {
        this.isFunctionComment = selected;
        return this;
    }

    public TamperingUtil withHexToChar(boolean selected) {
        this.isHexToChar = selected;
        return this;
    }

    public TamperingUtil withQuoteToUtf8(boolean selected) {
        this.isQuoteToUtf8 = selected;
        return this;
    }

    public TamperingUtil withRandomCase(boolean selected) {
        this.isRandomCase = selected;
        return this;
    }

    public TamperingUtil withSpaceToDashComment(boolean selected) {
        this.isSpaceToDashComment = selected;
        return this;
    }

    public TamperingUtil withSpaceToMultilineComment(boolean selected) {
        this.isSpaceToMultilineComment = selected;
        return this;
    }

    public TamperingUtil withSpaceToSharpComment(boolean selected) {
        this.isSpaceToSharpComment = selected;
        return this;
    }

    public TamperingUtil withStringToChar(boolean selected) {
        this.isStringToChar = selected;
        return this;
    }

    public TamperingUtil withVersionComment(boolean selected) {
        this.isVersionComment = selected;
        return this;
    }
}
