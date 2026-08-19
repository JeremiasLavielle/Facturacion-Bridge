package com.bridge.facturacion.pdf;

import com.bridge.facturacion.emisor.Emisor;
import com.bridge.facturacion.alumno.Alumno;
import com.bridge.facturacion.factura.EstadoFactura;
import com.bridge.facturacion.factura.Factura;
import com.bridge.facturacion.factura.FacturaRepository;
import com.bridge.facturacion.factura.exception.FacturaNoEmitidaException;
import com.bridge.facturacion.factura.exception.FacturaNotFoundException;
import com.bridge.facturacion.notacredito.EstadoNotaCredito;
import com.bridge.facturacion.notacredito.NotaCredito;
import com.bridge.facturacion.notacredito.NotaCreditoRepository;
import com.bridge.facturacion.notacredito.exception.NotaCreditoNotFoundException;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("MM/yyyy");
    // Para nombres de archivo: la barra de "MM/yyyy" no es válida, y el orden
    // año-mes hace que los PDF de un alumno queden cronológicos al ordenarlos
    // por nombre en una carpeta.
    private static final DateTimeFormatter PERIODO_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TIPO_FACTURA_C = 11;
    private static final int TIPO_NOTA_CREDITO_C = 13;
    private static final int DOC_TIPO_DNI = 96;

    private final FacturaRepository facturaRepository;
    private final NotaCreditoRepository notaCreditoRepository;

    public PdfService(FacturaRepository facturaRepository, NotaCreditoRepository notaCreditoRepository) {
        this.facturaRepository = facturaRepository;
        this.notaCreditoRepository = notaCreditoRepository;
    }

private record Comprobante(
            Emisor emisor, Alumno alumno, int tipo, String titulo,
            long numero, LocalDateTime fechaEmision, LocalDate periodo,
            BigDecimal monto, String cae, LocalDate vencimientoCae,
            String descripcionItem, String leyendaAsociada) {}

    private Comprobante de(Factura factura) {
        return new Comprobante(
                factura.getEmisor(), factura.getAlumno(), TIPO_FACTURA_C, "FACTURA",
                factura.getNumeroComprobante(), factura.getFechaEmision(), factura.getPeriodo(),
                factura.getMonto(), factura.getCae(), factura.getVencimientoCae(),
                "Servicios educativos — período " + PERIODO.format(factura.getPeriodo()),
                null);
    }

    private Comprobante de(NotaCredito nc) {
        Factura factura = nc.getFactura();
        return new Comprobante(
                nc.getEmisor(), factura.getAlumno(), TIPO_NOTA_CREDITO_C, "NOTA DE CRÉDITO",
                nc.getNumeroComprobante(), nc.getFechaEmision(), factura.getPeriodo(),
                nc.getMonto(), nc.getCae(), nc.getVencimientoCae(),
                "Anulación — servicios educativos período " + PERIODO.format(factura.getPeriodo()),
                "Anula a Factura C %04d-%08d".formatted(
                        nc.getEmisor().getPuntoVenta(), factura.getNumeroComprobante()));
    }

public String nombreArchivo(Factura factura) {
        return "%s - %s.pdf".formatted(
                limpiarParaNombreDeArchivo(factura.getAlumno().getNombre()),
                PERIODO_ARCHIVO.format(factura.getPeriodo()));
    }

    public String nombreArchivo(NotaCredito nc) {
        // Prefijo "NC" para que no colisione con el PDF de su factura, que
        // comparte alumno y periodo.
        return "NC %s - %s.pdf".formatted(
                limpiarParaNombreDeArchivo(nc.getFactura().getAlumno().getNombre()),
                PERIODO_ARCHIVO.format(nc.getFactura().getPeriodo()));
    }

    /**
     * Quita los caracteres que Windows y Linux no admiten en un nombre de
     * archivo. Sin esto, un alumno cargado como "Perez / Juan" generaria una
     * descarga rota o, peor, un nombre interpretado como ruta.
     */
    private String limpiarParaNombreDeArchivo(String nombre) {
        String limpio = nombre.replaceAll("[\\\\/:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return limpio.isEmpty() ? "comprobante" : limpio;
    }

    @Transactional(readOnly = true)
    public Factura buscarEmitida(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new FacturaNotFoundException(id));

        if (factura.getEstado() != EstadoFactura.EMITIDA
                && factura.getEstado() != EstadoFactura.ANULADA) {
            throw new FacturaNoEmitidaException(id);
        }

        if (factura.getNumeroComprobante() == null) {
            throw FacturaNoEmitidaException.sinNumeroComprobante(id);
        }
        return factura;
    }

    @Transactional(readOnly = true)
    public NotaCredito buscarNotaCreditoEmitida(Long id) {
        NotaCredito nc = notaCreditoRepository.findById(id)
                .orElseThrow(() -> new NotaCreditoNotFoundException(id));
        if (nc.getEstado() != EstadoNotaCredito.EMITIDA || nc.getNumeroComprobante() == null) {
            throw FacturaNoEmitidaException.notaCredito(id);
        }
        return nc;
    }

    public byte[] generar(Factura factura) {
        return generar(de(factura));
    }

    public byte[] generar(NotaCredito nc) {
        return generar(de(nc));
    }

    private byte[] generar(Comprobante cbte) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        try (Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(24, 28, 24, 28);
            doc.add(cabecera(cbte));
            doc.add(bloqueReceptor(cbte));
            doc.add(tablaItems(cbte));
            doc.add(espaciador(240));
            doc.add(totales(cbte));
            doc.add(espaciador(10));
            doc.add(pieConQr(cbte, pdf));
        }
        return out.toByteArray();
    }

private Table cabecera(Comprobante cbte) {
        Emisor emisor = cbte.emisor();
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{44, 12, 44}))
                .useAllAvailableWidth();

        Cell izquierda = celdaConBorde()
                .add(new Paragraph(emisor.getNombreFantasia())
                        .setBold().setFontSize(15).setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(8).setMarginBottom(10))
                .add(centrado(emisor.getRazonSocial()))
                .add(centrado(emisor.getDomicilio()))
                .add(new Paragraph(emisor.getCondicionFiscal())
                        .setBold().setFontSize(9).setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(4));

        Cell centro = celdaConBorde().setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("C").setBold().setFontSize(26)
                        .setBorder(new SolidBorder(1.2f)).setMarginTop(4)
                        .setPaddingLeft(6).setPaddingRight(6))
                .add(new Paragraph("CÓD. " + cbte.tipo()).setFontSize(7))
                .add(new Paragraph("ORIGINAL").setFontSize(8).setBold()
                        .setBackgroundColor(new DeviceGray(0.85f)));

        Cell derecha = celdaConBorde().setPaddingLeft(10)
                .add(new Paragraph(cbte.titulo()).setBold()
                        .setFontSize(cbte.tipo() == TIPO_NOTA_CREDITO_C ? 14 : 18).setMarginTop(4))
                .add(new Paragraph("%04d-%08d".formatted(emisor.getPuntoVenta(), cbte.numero()))
                        .setBold().setFontSize(11))
                .add(campo("Fecha de Emisión: ", FECHA.format(cbte.fechaEmision())))
                .add(new Paragraph("").setFontSize(6))
                .add(campo("CUIT: ", formatearCuit(emisor.getCuit())))
                .add(campo("Ingresos Brutos: ", emisor.getIngresosBrutos()))
                .add(campo("Inicio de Actividades: ", emisor.getInicioActividades()));

        tabla.addCell(izquierda).addCell(centro).addCell(derecha);
        return tabla;
    }

    private Table bloqueReceptor(Comprobante cbte) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginTop(6);
        tabla.addCell(celdaConBorde().setPaddingLeft(8)
                .add(campo("Nombre: ", cbte.alumno().getNombre()))
                .add(campo("Cond. IVA: ", legible(cbte.alumno().getCondicionIva().name())))
                .add(campo("Cond. Venta: ", "Contado")));
        Cell derecha = celdaConBorde().setPaddingLeft(8)
                .add(campo("DNI: ", cbte.alumno().getDni()))
                .add(campo("Período: ", PERIODO.format(cbte.periodo())));

        if (cbte.leyendaAsociada() != null) {
            derecha.add(new Paragraph(cbte.leyendaAsociada()).setBold().setFontSize(9));
        } else {
            derecha.add(new Paragraph(" ").setFontSize(9));
        }
        tabla.addCell(derecha);
        return tabla;
    }

    private Table tablaItems(Comprobante cbte) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{11, 49, 12, 14, 14}))
                .useAllAvailableWidth().setMarginTop(6);
        for (String titulo : new String[]{"Código", "Descripción", "Cantidad", "P. Unitario", "Importe"}) {
            tabla.addHeaderCell(new Cell()
                    .add(new Paragraph(titulo).setBold().setFontSize(9))
                    .setBackgroundColor(new DeviceGray(0.88f))
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(0.8f)).setBorderBottom(new SolidBorder(0.8f)));
        }
        String importe = moneda(cbte.monto());
        tabla.addCell(celdaItem("1", TextAlignment.RIGHT));
        tabla.addCell(celdaItem(cbte.descripcionItem(), TextAlignment.LEFT));
        tabla.addCell(celdaItem("1", TextAlignment.RIGHT));
        tabla.addCell(celdaItem(importe, TextAlignment.RIGHT));
        tabla.addCell(celdaItem(importe, TextAlignment.RIGHT));
        return tabla;
    }

    private Table totales(Comprobante cbte) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{62, 22, 16}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(0.8f));
        tabla.addCell(celdaSinBorde(""));
        tabla.addCell(celdaSinBorde("Subtotal: $").setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(moneda(cbte.monto())).setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(""));
        tabla.addCell(celdaSinBorde("Dto./Recargo: $").setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde("0,00").setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(""));
        tabla.addCell(celdaSinBorde("Total: $").setBold().setTextAlignment(TextAlignment.RIGHT));
        tabla.addCell(celdaSinBorde(moneda(cbte.monto())).setBold().setTextAlignment(TextAlignment.RIGHT));
        return tabla;
    }

    private Table pieConQr(Comprobante cbte, PdfDocument pdf) {
        Emisor emisor = cbte.emisor();
        String url = QrArca.buildUrl(
                cbte.fechaEmision().toLocalDate(),
                Long.parseLong(emisor.getCuit()),
                emisor.getPuntoVenta(),
                cbte.tipo(),
                cbte.numero(),
                cbte.monto(),
                DOC_TIPO_DNI,
                Long.parseLong(cbte.alumno().getDni()),
                cbte.cae());
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
                .add(campo("CAE Nº: ", cbte.cae()).setMarginTop(14))
                .add(campo("Fecha de Vto. de CAE: ", FECHA.format(cbte.vencimientoCae()))));
        return tabla;
    }

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
