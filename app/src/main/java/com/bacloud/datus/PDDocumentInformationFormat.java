package com.bacloud.datus;

import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;

public class PDDocumentInformationFormat {
    public static String formatDate(GregorianCalendar calendar) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");
        fmt.setCalendar(calendar);
        String dateFormatted = fmt.format(calendar.getTime());

        return dateFormatted;
    }
    public static ArrayList<String> format(PDDocumentInformation info){
        String title = info.getTitle() == null ? "" : ("<font color='#3498DB'>Title= </font>" + info.getTitle() + "<br>");
        String author = info.getAuthor() == null ? "" : ("<font color='#3498DB'>Author= </font>" + info.getAuthor() + "<br>");
        String subject = info.getSubject() == null ? "" : ("<font color='#3498DB'>Subject= </font>" + info.getSubject() + "<br>");
        String keywords = info.getKeywords() == null ? "" : ("<font color='#3498DB'>Keywords= </font>" + info.getKeywords() + "<br>");
        String creator = info.getCreator() == null ? "" : ("<font color='#3498DB'>Creator= </font>" + info.getCreator() + "<br>");
        String producer = info.getProducer() == null ? "" : ("<font color='#3498DB'>Producer= </font>" + info.getProducer() + "<br>");
        String creationDate = info.getCreationDate() == null ? "" : ("<font color='#3498DB'>Creation Date= </font>" + formatDate((GregorianCalendar) info.getCreationDate()) + "<br>");
        String modificationDate = info.getModificationDate() == null ? "" : ("<font color='#3498DB'>Modification Date= </font>" + formatDate((GregorianCalendar) info.getModificationDate()) + "<br>");
        String trapped = info.getTrapped() == null ? "" : ("<font color='#3498DB'>Trapped= </font>" + info.getTrapped() + "><br>");
        String[] metadata_ = {title, author, subject, keywords, creator, producer, creationDate, modificationDate, trapped};
        ArrayList<String> metadata = new ArrayList<String>();
        metadata.addAll(Arrays.asList(metadata_));
        return metadata;
    }
}
