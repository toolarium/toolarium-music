/*
 * GpSong.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A Guitar Pro song: metadata, master bars (measure headers) and tracks.
 *
 * @author patrick
 */
public class GpSong {
    private String version;
    private String title;
    private String subtitle;
    private String artist;
    private String album;
    private String words;
    private String music;
    private String copyright;
    private String tab;
    private String instructions;
    private int tempo = 120;
    private String tempoText;
    private int lyricsTrack = -1;
    private final List<GpLyricLine> lyrics = new ArrayList<>();
    private final List<GpMasterBar> masterBars = new ArrayList<>();
    private final List<GpTrack> tracks = new ArrayList<>();


    /**
     * Constructor for GpSong.
     */
    public GpSong() {
        // default constructor
    }


    /**
     * Gets the file format version, for example 5.10 or GP7.
     *
     * @return the file format version, for example 5.10 or GP7
     */
    public String getVersion() {
        return version;
    }


    /**
     * Sets the file format version, for example 5.10 or GP7.
     *
     * @param version the file format version, for example 5.10 or GP7
     */
    public void setVersion(String version) {
        this.version = version;
    }


    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }


    /**
     * Sets the title.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }


    /**
     * Gets the subtitle.
     *
     * @return the subtitle
     */
    public String getSubtitle() {
        return subtitle;
    }


    /**
     * Sets the subtitle.
     *
     * @param subtitle the subtitle
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }


    /**
     * Gets the artist.
     *
     * @return the artist
     */
    public String getArtist() {
        return artist;
    }


    /**
     * Sets the artist.
     *
     * @param artist the artist
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }


    /**
     * Gets the album.
     *
     * @return the album
     */
    public String getAlbum() {
        return album;
    }


    /**
     * Sets the album.
     *
     * @param album the album
     */
    public void setAlbum(String album) {
        this.album = album;
    }


    /**
     * Gets the lyricist.
     *
     * @return the lyricist
     */
    public String getWords() {
        return words;
    }


    /**
     * Sets the lyricist.
     *
     * @param words the lyricist
     */
    public void setWords(String words) {
        this.words = words;
    }


    /**
     * Gets the composer.
     *
     * @return the composer
     */
    public String getMusic() {
        return music;
    }


    /**
     * Sets the composer.
     *
     * @param music the composer
     */
    public void setMusic(String music) {
        this.music = music;
    }


    /**
     * Gets the copyright.
     *
     * @return the copyright
     */
    public String getCopyright() {
        return copyright;
    }


    /**
     * Sets the copyright.
     *
     * @param copyright the copyright
     */
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }


    /**
     * Gets the tab author (transcriber).
     *
     * @return the tab author (transcriber)
     */
    public String getTab() {
        return tab;
    }


    /**
     * Sets the tab author (transcriber).
     *
     * @param tab the tab author (transcriber)
     */
    public void setTab(String tab) {
        this.tab = tab;
    }


    /**
     * Gets the instructions.
     *
     * @return the instructions
     */
    public String getInstructions() {
        return instructions;
    }


    /**
     * Sets the instructions.
     *
     * @param instructions the instructions
     */
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }


    /**
     * Gets the initial tempo in quarter notes per minute.
     *
     * @return the initial tempo in quarter notes per minute
     */
    public int getTempo() {
        return tempo;
    }


    /**
     * Sets the initial tempo in quarter notes per minute.
     *
     * @param tempo the initial tempo in quarter notes per minute
     */
    public void setTempo(int tempo) {
        this.tempo = tempo;
    }


    /**
     * Gets the initial tempo text.
     *
     * @return the initial tempo text
     */
    public String getTempoText() {
        return tempoText;
    }


    /**
     * Sets the initial tempo text.
     *
     * @param tempoText the initial tempo text
     */
    public void setTempoText(String tempoText) {
        this.tempoText = tempoText;
    }


    /**
     * Gets the index of the track with the lyrics (-1 for none).
     *
     * @return the index of the track with the lyrics (-1 for none)
     */
    public int getLyricsTrack() {
        return lyricsTrack;
    }


    /**
     * Sets the index of the track with the lyrics (-1 for none).
     *
     * @param lyricsTrack the index of the track with the lyrics (-1 for none)
     */
    public void setLyricsTrack(int lyricsTrack) {
        this.lyricsTrack = lyricsTrack;
    }


    /**
     * Gets the lyrics lines (mutable).
     *
     * @return the lyrics lines (mutable)
     */
    public List<GpLyricLine> getLyrics() {
        return lyrics;
    }


    /**
     * Gets the master bars (mutable).
     *
     * @return the master bars (mutable)
     */
    public List<GpMasterBar> getMasterBars() {
        return masterBars;
    }


    /**
     * Gets the tracks (mutable).
     *
     * @return the tracks (mutable)
     */
    public List<GpTrack> getTracks() {
        return tracks;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                version, title, subtitle, artist, album, words, music, copyright, tab, instructions, tempo, tempoText,
                lyricsTrack, lyrics, masterBars, tracks);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpSong with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpSong other = (GpSong) obj;
        return Objects.equals(version, other.version)
                && Objects.equals(title, other.title)
                && Objects.equals(subtitle, other.subtitle)
                && Objects.equals(artist, other.artist)
                && Objects.equals(album, other.album)
                && Objects.equals(words, other.words)
                && Objects.equals(music, other.music)
                && Objects.equals(copyright, other.copyright)
                && Objects.equals(tab, other.tab)
                && Objects.equals(instructions, other.instructions)
                && Objects.equals(tempo, other.tempo)
                && Objects.equals(tempoText, other.tempoText)
                && Objects.equals(lyricsTrack, other.lyricsTrack)
                && Objects.equals(lyrics, other.lyrics)
                && Objects.equals(masterBars, other.masterBars)
                && Objects.equals(tracks, other.tracks);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpSong ["
                + "version=" + version
                + ", title=" + title
                + ", subtitle=" + subtitle
                + ", artist=" + artist
                + ", album=" + album
                + ", words=" + words
                + ", music=" + music
                + ", copyright=" + copyright
                + ", tab=" + tab
                + ", instructions=" + instructions
                + ", tempo=" + tempo
                + ", tempoText=" + tempoText
                + ", lyricsTrack=" + lyricsTrack
                + ", lyrics=" + lyrics
                + ", masterBars=" + masterBars
                + ", tracks=" + tracks
                + "]";
    }
}
