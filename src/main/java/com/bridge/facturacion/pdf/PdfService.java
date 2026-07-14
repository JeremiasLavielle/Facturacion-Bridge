package com.bridge.facturacion.pdf;

import com.bridge.facturacion.arca.ArcaProperties;
import com.bridge.facturacion.factura.EstadoFactura;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import com.bridge.facturacion.factura.exception.FacturaNoEmitidaException;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final int TIPO_FACTURA_C = 11;
    private static final int DOC_TIPO_DNI = 96;

    private final FacturaRepository facturaRepository;
    private final ArcaProperties arca;
    private final EmisorProperties emisor;

    public PdfService(FacturaRepository facturaRepository, ArcaProperties arca, EmisorProperties emisor) {
        this.facturaRepository = facturaRepository;
        this.arca = arca;
        this.emisor = emisor;
    }

    /** Nombre de archivo estilo "factura-0001-00000042.pdf". */
    public String nombreArchivo(Factura factura) {
        return "factura-%04d-%08d.pdf".formatted(arca.puntoVenta(), factura.getNumeroComprobante());
    }

    @Transactional(readOnly = true)
    public Factura buscarEmitida(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new FacturaNotFoundException(id));
        if (factura.getEstado() != EstadoFactura.EMITIDA) {
            throw new FacturaNoEmitidaException(id);
        }

        if (factura.getNumeroComprobante() == null) {
            throw FacturaNoEmitidaException.sinNumeroComprobante(id);
        }
        return factura;
    }

    public byte[] generar(Factura factura) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        try (Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(24, 28, 24, 28);
            doc.add(cabecera(factura));
            doc.add(bloqueReceptor(factura));
            doc.add(tablaItems(factura));
            doc.add(espaciador(240));
            doc.add(totales(factura));
            doc.add(espaciador(10));
            doc.add(pieConQr(factura, pdf));
        }
        return out.toByteArray();
    }

    // ---------------- secciones ----------------

    private Table cabecera(Factura factura) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{44, 12, 44}))
                .useAllAvailableWidth();

        Cell izquierda = celdaConBorde()
                .add(new Paragraph(emisor.nombreFantasia())
                        .setBold().setFontSize(15).setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(8).setMarginBottom(10))
                .add(centrado(emisor.razonSocial()))
                .add(centrado(emisor.domicilio()))
                .add(new Paragraph(emisor.condicionFiscal())
                        .setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(4));

        Cell centro = celdaConBorde().setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("C").setBold().setFontSize(26)
                        .setBorder(new SolidBorder(1.2f)).setMarginTop(4)
                        .setPaddingLeft(6).setPaddingRight(6))
                .add(new Paragraph("CÓD. " + TIPO_FACTURA_C).setFontSize(7))
                .add(new Paragraph("ORIGINAL").setFontSize(8).setBold()
                        .setBackgroundColor(new DeviceGray(0.85f)));

        Cell derecha = celdaConBorde().setPaddingLeft(10)
                .add(new Paragraph("FACTURA").setBold().setFontSize(18).setMarginTop(4))
                .add(new Paragraph("%04d-%08d".formatted(arca.puntoVenta(), factura.getNumeroComprobante()))
                        .setBold().setFontSize(11))
                .add(campo("Fecha de Emisión: ", FECHA.format(factura.getFechaEmision())))
                .add(new Paragraph("").setFontSize(6))
                .add(campo("CUIT: ", formatearCuit(arca.cuitEmisor())))
                .add(campo("Ingresos Brutos: ", emisor.ingresosBrutos()))
                .add(campo("Inicio de Actividades: ", emisor.inicioActividades()));

        tabla.addCell(izquierda).addCell(centro).addCell(derecha);
        return tabla;
    }

    private Table bloqueReceptor(Factura factura) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginTop(6);
        tabla.addCell(celdaConBorde().setPaddingLeft(8)
                .add(campo("Nombre: ", factura.getAlumno().getNombre()))
                .add(campo("Cond. IVA: ", legible(factura.getAlumno().getCondicionIva().name())))
                .add(campo("Cond. Venta: ", "Contado")));
        tabla.addCell(celdaConBorde().setPaddingLeft(8)
                .add(campo("DNI: ", factura.getAlumno().getDni()))
                .add(campo("Período: ", PERIODO.format(factura.getPeriodo())))
                .add(new Paragraph(" ").setFontSize(9)));
        return tabla;
    }

    private Table tablaItems(Factura factura) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{11, 49, 12, 14, 14}))
                .useAllAvailableWidth().setMarginTop(6);
        for (String titulo : new String[]{"Código", "Descripción", "Cantidad", "P. Unitario", "Importe"}) {
            tabla.addHeaderCell(new Cell()
                    .add(new Paragraph(titulo).setBold().setFontSize(9))
                    .setBackgroundColor(new DeviceGray(0.88f))
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(0.8f)).setBorderBottom(new SolidBorder(0.8f)));
        }
        String descripcion = "Servicios educativos — período " + PERIODO.format(factura.getPeriodo());
        String importe = moneda(factura.getMonto());
        tabla.addCell(celdaItem("1", TextAlignment.RIGHT));
        tabla.addCell(celdaItem(descripcion, TextAlignment.LEFT));
        tabla.addCell(celdaItem("1", TextAlignment.RIGHT));
        tabla.addCell(celdaItem(importe, TextAlignment.RIGHT));
        tabla.addCell(celdaItem(importe, TextAlignment.RIGHT));
        return tabla;
    }

    private Table totales(Factura factura) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{62, 22, 16}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(0.8f));
        tabla.addCell(celdaSinBorde(""));
        tabla.addCell(celdaSinBorde("Subtotal: $").setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(moneda(factura.getMonto())).setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(""));
        tabla.addCell(celdaSinBorde("Dto./Recargo: $").setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde("0,00").setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(""));
        tabla.addCell(celdaSinBorde("Total: $").setBold().setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(moneda(factura.getMonto())).setBold().setTextAlignment(TextAlignment.RIGHT));
        return tabla;
    }

    private Table pieConQr(Factura factura, PdfDocument pdf) {
        String url = QrArca.buildUrl(
                factura.getFechaEmision().toLocalDate(),
                Long.parseLong(arca.cuitEmisor()),
                arca.puntoVenta(),
                TIPO_FACTURA_C,
                factura.getNumeroComprobante(),
                factura.getMonto(),
                DOC_TIPO_DNI,
                Long.parseLong(factura.getAlumno().getDni()),
                factura.getCae());
        Image qr = new Image(new BarcodeQRCode(url).createFormXObject(ColorConstants.BLACK, pdf))
                .setWidth(85).setHeight(85);

        Table tabla = new Table(UnitValue.createPercentArray(new float[]{16, 44, 40}))
                .useAllAvailableWidth().setMarginTop(8);
        tabla.addCell(new Cell().setBorder(Border.NO_BORDER).add(qr));
        tabla.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("ARCA").setBold().setFontSize(16).setMarginTop(14))
                .add(new Paragraph("Comprobante Autorizado").setBold().setItalic().setFontSize(10))
                .add(new Paragraph("Esta Administración Federal no se responsabiliza por los datos "
                        + "ingresados en el detalle de la operación").setFontSize(6.5f).setItalic()));
        tabla.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .add(campo("CAE Nº: ", factura.getCae()).setMarginTop(14))
                .add(campo("Fecha de Vto. de CAE: ", FECHA.format(factura.getVencimientoCae()))));
        return tabla;
    }

    // ---------------- helpers de formato ----------------

    private Cell celdaConBorde() {
        return new Cell().setBorder(new SolidBorder(0.8f)).setPadding(4);
    }

    private Cell celdaSinBorde(String texto) {
        return new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(texto).setFontSize(10));
    }

    private Cell celdaItem(String texto, TextAlignment alineacion) {
        return new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(texto).setFontSize(9).setTextAlignment(alineacion));
    }

    private Paragraph centrado(String texto) {
        return new Paragraph(texto).setFontSize(9).setTextAlignment(TextAlignment.CENTER);
    }

    private Paragraph campo(String etiqueta, String valor) {
        return new Paragraph().setFontSize(9)
                .add(new Text(etiqueta).setBold())
                .add(new Text(valor == null ? "-" : valor));
    }

    private Paragraph espaciador(float alto) {
        return new Paragraph(" ").setHeight(alto);
    }

    private String moneda(java.math.BigDecimal monto) {
        DecimalFormat formato = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));
        return formato.format(monto);
    }

    private String formatearCuit(String cuit) {
        return cuit.length() == 11
                ? cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10)
                : cuit;
    }

    private String legible(String enumName) {
        String[] palabras = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
