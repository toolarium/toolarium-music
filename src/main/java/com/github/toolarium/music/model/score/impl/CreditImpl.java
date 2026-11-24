/*
 * CreditImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.score.ICredit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link ICredit} interface.
 *
 * @author patrick
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class CreditImpl implements ICredit {

    @JacksonXmlProperty(isAttribute = true, localName = "page")
    private Integer page;

    @JacksonXmlProperty(localName = "credit-type")
    private String creditType;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "credit-words")
    private List<CreditWordsImpl> creditWords;


    /**
     * Gets the page.
     *
     * @return the page
     */
    @Override
    public Integer getPage() {
        return page;
    }


    /**
     * Sets the page.
     *
     * @param page the page
     */
    public void setPage(Integer page) {
        this.page = page;
    }


    /**
     * Gets the credit type.
     *
     * @return the credit type
     */
    @Override
    public String getCreditType() {
        return creditType;
    }


    /**
     * Sets the credit type.
     *
     * @param creditType the credit type
     */
    public void setCreditType(String creditType) {
        this.creditType = creditType;
    }


    /**
     * Gets the first credit words: the words with the position of the credit.
     *
     * @return the first credit words, or null if there are none
     */
    @Override
    public CreditWordsImpl getCreditWords() {
        if (creditWords == null || creditWords.isEmpty()) {
            return null;
        }
        return creditWords.get(0);
    }


    /**
     * Sets the credit words as the only credit words of the credit.
     *
     * @param creditWords the credit words
     */
    public void setCreditWords(CreditWordsImpl creditWords) {
        if (creditWords == null) {
            this.creditWords = null;
        } else {
            this.creditWords = new ArrayList<>();
            this.creditWords.add(creditWords);
        }
    }


    /**
     * Gets all credit words: the words after the first one continue its text with their own font.
     *
     * @return the credit words, or null if there are none
     */
    @Override
    public List<CreditWordsImpl> getCreditWordsList() {
        return creditWords;
    }


    /**
     * Sets all credit words.
     *
     * @param creditWords the credit words
     */
    public void setCreditWordsList(List<CreditWordsImpl> creditWords) {
        this.creditWords = creditWords;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(page, creditType, creditWords);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CreditImpl other = (CreditImpl) obj;
        return Objects.equals(page, other.page)
                && Objects.equals(creditType, other.creditType)
                && Objects.equals(creditWords, other.creditWords);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "CreditImpl [page=" + page
                + ", creditType=" + creditType
                + ", creditWords=" + creditWords + "]";
    }
}
