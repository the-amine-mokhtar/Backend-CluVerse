package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipCertificate;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CertificatePdfService {

    public byte[] generateCertificatePdf(MentorshipCertificate certificate) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setNonStrokingColor(0.98f, 0.98f, 1.0f);
                contentStream.addRect(0, 0, pageWidth, pageHeight);
                contentStream.fill();

                contentStream.setNonStrokingColor(0.5f, 0.3f, 0.8f);
                contentStream.addRect(0, pageHeight - 80, pageWidth, 80);
                contentStream.fill();

                contentStream.setNonStrokingColor(0.5f, 0.3f, 0.8f);
                contentStream.addRect(0, 0, pageWidth, 40);
                contentStream.fill();

                contentStream.setNonStrokingColor(0.95f, 0.75f, 0.3f);
                contentStream.addRect(0, pageHeight - 85, pageWidth, 5);
                contentStream.fill();

                contentStream.addRect(0, 35, pageWidth, 5);
                contentStream.fill();

                contentStream.setLineWidth(4);
                contentStream.setStrokingColor(0.4f, 0.2f, 0.7f);
                contentStream.addRect(25, 25, pageWidth - 50, pageHeight - 50);
                contentStream.stroke();

                contentStream.setLineWidth(2);
                contentStream.setStrokingColor(0.7f, 0.5f, 0.9f);
                contentStream.addRect(35, 35, pageWidth - 70, pageHeight - 70);
                contentStream.stroke();

                contentStream.setLineWidth(3);
                contentStream.setStrokingColor(0.95f, 0.75f, 0.3f);

                contentStream.moveTo(45, pageHeight - 45);
                contentStream.lineTo(45, pageHeight - 100);
                contentStream.lineTo(100, pageHeight - 100);
                contentStream.stroke();

                contentStream.moveTo(pageWidth - 45, pageHeight - 45);
                contentStream.lineTo(pageWidth - 45, pageHeight - 100);
                contentStream.lineTo(pageWidth - 100, pageHeight - 100);
                contentStream.stroke();

                contentStream.moveTo(45, 45);
                contentStream.lineTo(45, 100);
                contentStream.lineTo(100, 100);
                contentStream.stroke();

                contentStream.moveTo(pageWidth - 45, 45);
                contentStream.lineTo(pageWidth - 45, 100);
                contentStream.lineTo(pageWidth - 100, 100);
                contentStream.stroke();

                contentStream.setNonStrokingColor(1.0f, 1.0f, 1.0f);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 38);
                contentStream.beginText();
                contentStream.newLineAtOffset(pageWidth / 2 - 121, pageHeight - 50);
                contentStream.showText("CERTIFICATE");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(pageWidth / 2 - 154, pageHeight - 95);
                contentStream.showText("OF COMPLETION");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.9f, 0.85f, 1.0f);
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(pageWidth / 2 - 108, pageHeight - 140);
                contentStream.showText("MENTORSHIP ACHIEVEMENT AWARD");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.3f, 0.3f, 0.5f);
                contentStream.setFont(PDType1Font.HELVETICA, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(pageWidth / 2 - 85, pageHeight - 200);
                contentStream.showText("This is to certify that");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.4f, 0.2f, 0.7f);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 32);
                String menteeName = certificate.getMentee().getFirstName() + " " + certificate.getMentee().getLastName();
                float menteeWidth = menteeName.length() * 18;
                float menteeX = (pageWidth - menteeWidth) / 2;
                contentStream.beginText();
                contentStream.newLineAtOffset(menteeX, pageHeight - 260);
                contentStream.showText(menteeName);
                contentStream.endText();

                contentStream.setLineWidth(2);
                contentStream.setStrokingColor(0.95f, 0.75f, 0.3f);
                contentStream.moveTo(menteeX - 10, pageHeight - 275);
                contentStream.lineTo(menteeX + menteeWidth + 10, pageHeight - 275);
                contentStream.stroke();

                contentStream.setNonStrokingColor(0.4f, 0.4f, 0.6f);
                contentStream.setFont(PDType1Font.HELVETICA, 15);
                contentStream.beginText();
                contentStream.newLineAtOffset(pageWidth / 2 - 130, pageHeight - 310);
                contentStream.showText("has successfully completed the mentorship program in");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.4f, 0.2f, 0.7f);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 28);
                String skillName = certificate.getSkillName();
                float skillWidth = skillName.length() * 16;
                float skillX = (pageWidth - skillWidth) / 2;
                contentStream.beginText();
                contentStream.newLineAtOffset(skillX, pageHeight - 360);
                contentStream.showText(skillName);
                contentStream.endText();

                contentStream.setLineWidth(2);
                contentStream.setStrokingColor(0.95f, 0.75f, 0.3f);
                contentStream.moveTo(skillX - 10, pageHeight - 375);
                contentStream.lineTo(skillX + skillWidth + 10, pageHeight - 375);
                contentStream.stroke();

                contentStream.setNonStrokingColor(0.95f, 0.93f, 0.98f);
                contentStream.addRect(100, pageHeight - 440, pageWidth - 200, 50);
                contentStream.fill();
                contentStream.setLineWidth(1);
                contentStream.setStrokingColor(0.7f, 0.5f, 0.9f);
                contentStream.stroke();

                contentStream.setNonStrokingColor(0.3f, 0.3f, 0.5f);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                float descWidth = certificate.getDescription().length() * 7;
                float descX = (pageWidth - descWidth) / 2;
                contentStream.newLineAtOffset(descX, pageHeight - 415);
                contentStream.showText(certificate.getDescription());
                contentStream.endText();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                String dateStr = certificate.getIssuedAt().format(formatter);
                String dateText = "Date of Issue: " + dateStr;
                contentStream.setNonStrokingColor(0.4f, 0.4f, 0.6f);
                contentStream.setFont(PDType1Font.HELVETICA, 13);
                contentStream.beginText();
                float dateWidth = dateText.length() * 8;
                float dateX = (pageWidth - dateWidth) / 2;
                contentStream.newLineAtOffset(dateX, pageHeight - 480);
                contentStream.showText(dateText);
                contentStream.endText();

                String certIdText = "Certificate ID: " + certificate.getId();
                contentStream.setNonStrokingColor(0.6f, 0.6f, 0.8f);
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                float certIdWidth = certIdText.length() * 6;
                float certIdX = (pageWidth - certIdWidth) / 2;
                contentStream.newLineAtOffset(certIdX, pageHeight - 500);
                contentStream.showText(certIdText);
                contentStream.endText();

                contentStream.setNonStrokingColor(0.4f, 0.4f, 0.6f);
                contentStream.setFont(PDType1Font.HELVETICA, 13);
                contentStream.beginText();
                contentStream.newLineAtOffset(150, pageHeight - 560);
                contentStream.showText("Presented by Mentor:");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.4f, 0.2f, 0.7f);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                String mentorName = certificate.getMentor().getFirstName() + " " + certificate.getMentor().getLastName();
                contentStream.beginText();
                contentStream.newLineAtOffset(150, pageHeight - 590);
                contentStream.showText(mentorName);
                contentStream.endText();

                contentStream.setLineWidth(2);
                contentStream.setStrokingColor(0.4f, 0.2f, 0.7f);
                contentStream.moveTo(150, pageHeight - 610);
                contentStream.lineTo(350, pageHeight - 610);
                contentStream.stroke();

                contentStream.setNonStrokingColor(0.6f, 0.6f, 0.8f);
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(220, pageHeight - 625);
                contentStream.showText("Signature");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.5f, 0.3f, 0.8f);
                contentStream.setLineWidth(3);
                float sealX = pageWidth - 120;
                float sealY = pageHeight - 550;
                float sealSize = 70;

                contentStream.addRect(sealX, sealY, sealSize, sealSize);
                contentStream.stroke();

                contentStream.setLineWidth(1);
                contentStream.addRect(sealX + 10, sealY + 10, sealSize - 20, sealSize - 20);
                contentStream.stroke();

                contentStream.setNonStrokingColor(0.4f, 0.2f, 0.7f);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(sealX - 25, sealY + 35);
                contentStream.showText("OFFICIAL");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(sealX - 20, sealY + 20);
                contentStream.showText("SEAL");
                contentStream.endText();

                contentStream.setNonStrokingColor(0.8f, 0.8f, 0.95f);
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(pageWidth / 2 - 100, 20);
                contentStream.showText("This certificate is awarded for outstanding achievement in mentorship");
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}