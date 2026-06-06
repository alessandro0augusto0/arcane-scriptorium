package com.arcane.scriptorium.ui.simulacao;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.arcane.scriptorium.simulation.SimulationEngine;
import com.arcane.scriptorium.synchronization.SynchronizationSnapshot;
import com.arcane.scriptorium.simulation.ArcaneAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.awt.Color;

public class PdfReportGenerator {

    public static void generateReport(File file, SimulationEngine engine) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(74, 20, 140)); // Purple
        Font subtitleFont = new Font(Font.HELVETICA, 12, Font.ITALIC, Color.DARK_GRAY);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font textFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        // Title
        Paragraph title = new Paragraph("Biblioteca Arcana - Relatorio de Simulacao", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Date
        Paragraph date = new Paragraph("Gerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), subtitleFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(20);
        document.add(date);

        // Metrics Table
        PdfPTable table = new PdfPTable(new float[]{3f, 2f, 1f, 1.5f, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(20f);

        String[] headers = {"Processo", "Tipo", "Acessos", "Espera Total", "Espera Media", "Espera Max"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new Color(103, 58, 183)); // Deep purple
            cell.setPadding(5);
            table.addCell(cell);
        }

        engine.agents().stream()
                .map(ArcaneAgent::metrics)
                .sorted(Comparator.comparingInt(m -> m.process().id()))
                .forEach(m -> {
                    table.addCell(new Phrase(m.process().shortName(), textFont));
                    table.addCell(new Phrase(m.process().role().displayName(), textFont));
                    
                    PdfPCell cellAcc = new PdfPCell(new Phrase(String.valueOf(m.accesses()), textFont));
                    cellAcc.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cellAcc);
                    
                    PdfPCell cellTotal = new PdfPCell(new Phrase(m.totalWaitMillis() + "ms", textFont));
                    cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellTotal);
                    
                    PdfPCell cellAvg = new PdfPCell(new Phrase(m.averageWaitMillis() + "ms", textFont));
                    cellAvg.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellAvg);
                    
                    PdfPCell cellMax = new PdfPCell(new Phrase(m.maxWaitMillis() + "ms", textFont));
                    cellMax.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellMax);
                });

        document.add(table);

        // Final Snapshot
        SynchronizationSnapshot snap = engine.finalSnapshot();
        Font boldText = new Font(Font.HELVETICA, 10, Font.BOLD);
        
        document.add(new Paragraph("Resumo do Estado Final", titleFont));
        document.add(new Paragraph(" "));
        document.add(new Phrase("Leituras concluidas: ", boldText));
        document.add(new Phrase(String.valueOf(snap.completedReads()) + "\n", textFont));
        document.add(new Phrase("Escritas concluidas: ", boldText));
        document.add(new Phrase(String.valueOf(snap.completedWrites()) + "\n", textFont));
        document.add(new Phrase("Leitores comuns esperando: ", boldText));
        document.add(new Phrase(String.valueOf(snap.waitingCommonReaders()) + "\n", textFont));
        document.add(new Phrase("Leitores criticos esperando: ", boldText));
        document.add(new Phrase(String.valueOf(snap.waitingCriticalReaders()) + "\n", textFont));
        document.add(new Phrase("Escritores esperando: ", boldText));
        document.add(new Phrase(String.valueOf(snap.waitingWriters()) + "\n", textFont));

        document.close();
    }
}
