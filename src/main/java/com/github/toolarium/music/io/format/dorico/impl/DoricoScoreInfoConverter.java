/*
 * DoricoScoreInfoConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import com.github.toolarium.music.io.converter.EncodingConverter;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import com.github.toolarium.music.model.score.impl.WorkImpl;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Converts the project and flow information of a Dorico project: work, creators, copyright, encoding and the
 * texts of the pages (title page texts and the header and page numbers of Dorico's default page template).
 *
 * @author patrick
 */
public final class DoricoScoreInfoConverter {
    private static final Logger LOG = LoggerFactory.getLogger(DoricoScoreInfoConverter.class);
    private static final int BARS_PER_SYSTEM = 4;
    private static final int CREDIT_BOTTOM = 75;
    private static final int CREDIT_TITLE_SIZE = 24;
    private static final int CREDIT_TOP = 1659;
    private static final int HEADER_TOP = 1678;
    private static final int PAGE_CENTER = 620;
    private static final int PAGE_LEFT = 165;
    private static final int PAGE_RIGHT = 1165;
    private static final int STAVES_PER_PAGE = 10;

    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DoricoScoreInfoConverter INSTANCE = new DoricoScoreInfoConverter();
    }


    /**
     * Constructor.
     */
    private DoricoScoreInfoConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DoricoScoreInfoConverter getInstance() {
        return HOLDER.INSTANCE;
    }



    /**
     * Builds work, movement title and identification from the project info.
     *
     * @param info the info node (may be null)
     * @param flow the converted flow (may be null)
     * @param score the score to populate
     */
    void buildIdentification(DoricoTreeNode info, DoricoTreeNode flow, ScoreImpl score) {
        IdentificationImpl identification = new IdentificationImpl();
        EncodingImpl encoding = new EncodingImpl();
        List<String> software = new ArrayList<>();
        software.add(EncodingConverter.getInstance().getSoftware());

        String title = null;
        if (info != null) {
            title = nonEmpty(info.getPathValue("title"));
            String lastSavedVersion = nonEmpty(info.getPathValue("lastSavedVersion"));
            if (lastSavedVersion != null) {
                software.add("Dorico " + lastSavedVersion);
            }

            String lastSavedDate = nonEmpty(info.getPathValue("lastSavedDate"));
            if (lastSavedDate != null) {
                try {
                    long seconds = Long.parseLong(lastSavedDate);
                    encoding.setEncodingDate(Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate().toString());
                } catch (RuntimeException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring invalid Dorico lastSavedDate: {}", lastSavedDate, e);
                    }
                }
            }

            // Dorico's MusicXML import fills the information of the flow, not of the project
            List<CreatorImpl> creators = new ArrayList<>();
            addCreator(creators, "composer", infoValue(info, flow, "composer"));
            addCreator(creators, "arranger", infoValue(info, flow, "arranger"));
            addCreator(creators, "lyricist", infoValue(info, flow, "lyricist"));
            if (!creators.isEmpty()) {
                identification.setCreators(creators);
            }

            String copyright = nonEmpty(infoValue(info, flow, "copyright"));
            if (copyright != null) {
                identification.setRights(copyright);
            }
        }

        if (title == null && flow != null) {
            title = nonEmpty(flow.getPathValue("flowInfo", "title"));
            if (title == null) {
                title = nonEmpty(flow.getPathValue("flowName"));
            }
        }

        if (title != null) {
            WorkImpl work = new WorkImpl();
            work.setWorkTitle(title);
            if (info != null) {
                String workNumber = nonEmpty(info.getPathValue("workNumber"));
                if (workNumber != null) {
                    work.setWorkNumber(workNumber);
                }
            }
            score.setWork(work);
            score.setMovementTitle(title);
        }

        encoding.setSoftware(software);
        identification.setEncoding(encoding);
        score.setIdentification(identification);
        if (info != null) {
            score.setCredits(buildCredits(title, info, flow));
        }
    }


    /**
     * Builds the texts of the first page from the project (or flow) information, placed on an A4 page in tenths:
     * the title and the subtitle at the top in the middle, the composer and the arranger at the top on the right
     * and the copyright at the bottom in the middle.
     *
     * @param title the title, or null
     * @param info the project information
     * @param flow the first flow, or null
     * @return the credits
     */
    private List<CreditImpl> buildCredits(String title, DoricoTreeNode info, DoricoTreeNode flow) {
        List<CreditImpl> result = new ArrayList<>();
        addCredit(result, "title", title, new int[] {PAGE_CENTER, CREDIT_TOP}, CREDIT_TITLE_SIZE);
        addCredit(result, "subtitle", infoValue(info, flow, "subtitle"), new int[] {PAGE_CENTER, CREDIT_TOP - CREDIT_TITLE_SIZE * 3}, null);
        String composer = infoValue(info, flow, "composer");
        String arranger = infoValue(info, flow, "arranger");
        if (arranger != null) {
            String line = "arr. " + arranger;
            if (composer == null) {
                composer = line;
            } else {
                composer = composer + "\n" + line;
            }
        }
        addCredit(result, "composer", composer, new int[] {PAGE_RIGHT, CREDIT_TOP - CREDIT_TITLE_SIZE * 3}, null);
        addCredit(result, "rights", infoValue(info, flow, "copyright"), new int[] {PAGE_CENTER, CREDIT_BOTTOM}, null);
        return result;
    }


    /**
     * Adds the texts of Dorico's default page template to the pages after the first page: the flow title as header
     * in the middle and the page number (on the left of left pages and on the right of right pages). Dorico does not
     * store the number of pages, it is estimated like Dorico lays out the score: bars per system and systems per page
     * depending on the number of staves.
     *
     * @param score the score (its credits are updated)
     * @param staves the number of staves
     * @param bars the number of bars
     */
    void addPageTexts(ScoreImpl score, int staves, int bars) {
        if (score.getCredits() == null || staves <= 0 || bars <= 0) {
            return;
        }
        int barsPerPage = BARS_PER_SYSTEM * Math.max(1, STAVES_PER_PAGE / staves);
        int pages = (bars + barsPerPage - 1) / barsPerPage;
        List<CreditImpl> credits = new ArrayList<>(score.getCredits());
        for (int page = 2; page <= pages; page++) {
            if (score.getMovementTitle() != null) {
                credits.add(createPageCredit(null, score.getMovementTitle(), page, new int[] {PAGE_CENTER, HEADER_TOP}, "center"));
            }
            if (page % 2 == 0) {
                credits.add(createPageCredit("page number", String.valueOf(page), page, new int[] {PAGE_LEFT, HEADER_TOP}, "left"));
            } else {
                credits.add(createPageCredit("page number", String.valueOf(page), page, new int[] {PAGE_RIGHT, HEADER_TOP}, "right"));
            }
        }
        score.setCredits(credits);
    }


    /**
     * Creates a text at the top of a page.
     *
     * @param type the credit type, or null for none
     * @param text the text
     * @param page the page number
     * @param position the default-x and default-y in tenths
     * @param justify the justification
     * @return the credit
     */
    private CreditImpl createPageCredit(String type, String text, int page, int[] position, String justify) {
        CreditWordsImpl words = new CreditWordsImpl();
        words.setValue(text);
        words.setDefaultX(String.valueOf(position[0]));
        words.setDefaultY(String.valueOf(position[1]));
        words.setJustify(justify);
        words.setValign("top");
        CreditImpl credit = new CreditImpl();
        credit.setPage(page);
        if (type != null) {
            credit.setCreditType(type);
        }
        credit.setCreditWords(words);
        return credit;
    }


    /**
     * Adds a text of the first page.
     *
     * @param credits the credits (updated)
     * @param type the credit type
     * @param text the text, or null for none
     * @param position the default-x and default-y in tenths (the right edge for a text on the right)
     * @param fontSize the font size, or null for the default size
     */
    private void addCredit(List<CreditImpl> credits, String type, String text, int[] position, Integer fontSize) {
        if (text == null) {
            return;
        }
        CreditWordsImpl words = new CreditWordsImpl();
        words.setValue(text);
        words.setDefaultX(String.valueOf(position[0]));
        words.setDefaultY(String.valueOf(position[1]));
        if (position[0] == PAGE_RIGHT) {
            words.setJustify("right");
        } else {
            words.setJustify("center");
        }
        if (position[1] == CREDIT_BOTTOM) {
            words.setValign("bottom");
        } else {
            words.setValign("top");
        }
        if (fontSize != null) {
            words.setFontSize(String.valueOf(fontSize));
        }
        CreditImpl credit = new CreditImpl();
        credit.setPage(1);
        credit.setCreditType(type);
        credit.setCreditWords(words);
        credits.add(credit);
    }


    /**
     * Gets a value of the project information, or of the information of the first flow if the project has none.
     *
     * @param info the project information
     * @param flow the first flow, or null
     * @param name the name of the value, for example composer
     * @return the value, or null if both are empty
     */
    private String infoValue(DoricoTreeNode info, DoricoTreeNode flow, String name) {
        String value = nonEmpty(info.getPathValue(name));
        if (value == null && flow != null) {
            value = nonEmpty(flow.getPathValue("flowInfo", name));
        }
        return value;
    }


    /**
     * Adds a creator if the value is not empty.
     *
     * @param creators the creators (updated)
     * @param type the creator type
     * @param value the creator, or null
     */
    private void addCreator(List<CreatorImpl> creators, String type, String value) {
        String text = nonEmpty(value);
        if (text != null) {
            CreatorImpl creator = new CreatorImpl();
            creator.setType(type);
            creator.setValue(text);
            creators.add(creator);
        }
    }


    /**
     * Returns the trimmed value, or null if it is empty.
     *
     * @param value the value (may be null)
     * @return the trimmed value, or null
     */
    private String nonEmpty(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
