package cl.facele.docele.transformer.logica;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.Long;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TransformerIECV {
	public static Logger logger = Logger.getLogger(Object.class);
	private List<List<XSSFCell>> listExcel;
	private Map<String, String> caratula = new HashMap<String, String>();
	private Map<String, Map<String, Long>> resumen = new HashMap<String, Map<String, Long>>();;
	private Collection<Map<String, String>> detalles = new ArrayList<Map<String, String>> ( );
	String proporcionalidad = "0";
	
	private boolean docSoloResumen(String tipoDoc) {
		if ("35,38,39,41,105,919,920,922,924".contains(tipoDoc))
			return true;
		else
			return false;
	}

	private String getAnulado(XSSFCell xssfCell) {
		String result;
		if (xssfCell == null)
			result =  "";
		else
			result = xssfCell.getErrorCellString();
		
		if (result.toLowerCase().contains("anula"))
			return "A";
		return "";
	}


	private String getRUT(String rut) throws Exception {
		String result = rut;
		try {
			if (result.contains("."))
				result = result.replace(".", "");
			if (result.contains(","))
				result = result.replace(",", "");
			if (result.contains(" "))
				result = result.replace(" ", "");
			
			while (result.startsWith("0")) {
				if (result.equals("0-0"))
					break;
				
				result = result.substring(1).trim();
			}
			
			new RUT(result);
		}catch (Exception e) {
			throw new Exception("ERROR validando RUT [" +
					rut + "]: " + e.getMessage());
		}
		return result;		
	}

	private String getRznSocial(String rznSocial) {
		String result = rznSocial;
		if (result.contains(";"))
			result = result.replaceAll(";", "");
		
		if (result.length() > 50)
			return result.substring(0, 47) + "...";
		return result;
	}

	public String getTXT(File fileExcel) throws Exception {
		logger.debug("Start...");
                
                
		if (fileExcel.exists())
			logger.info("file existe: " + fileExcel.getAbsolutePath());
		
		leeExcel(fileExcel);

		int elementoNumber = 100;		//es caratula
		for(List<XSSFCell> rawExcel: listExcel) {
			if (rawExcel.get(0) == null || rawExcel.get(0).toString().toString().isEmpty())
				continue;

			logger.debug(rawExcel);
			if(rawExcel.get(0).toString().equals("RESUMEN")) {
				if (elementoNumber != 100)
					throw new Exception("ERROR de formato de EXCEL. Esta repetido el 'RESUMEN'");
				
				elementoNumber = 200;	//es resumen
				continue;
			}

			if(rawExcel.get(0).toString().equals("DETALLE")) {
				if (elementoNumber != 200)
					throw new Exception("ERROR de formato de EXCEL. RESUMEN debe estar a continuacion de la CARATULA");
				
				elementoNumber = 300;	//es detalle
				continue;
			}
			
			if (elementoNumber == 100) {
				procesaEncabezado(rawExcel);
				continue;
			}
			
			if (elementoNumber == 200) {
				procesaResumen(rawExcel);
				continue;
			}
			
			if (elementoNumber == 300) {
				procesaDetalle(rawExcel);
				continue;
			}
		}
		
		
		return toTXT();
	}

	@SuppressWarnings("unchecked")
	private String toTXT() throws Exception {
		String _caratula = "";
		String _resumen = "";
		String _detalle = "";		
		
		try {
			//CARATULA
			_caratula += "A" + ";";
			_caratula += caratula.get("Tipo_Operacion") + ";";	//1 Tipo de operaci�n
			_caratula += "MENSUAL" + ";";	//2 Tipo Libro
			_caratula += caratula.get("RUT_Emisor") + ";";	//3 Rut Contribuyente
			_caratula += caratula.get("Periodo") + ";";	//4 Per�odo tributario
			_caratula += "TOTAL" + ";";	//5 Tipo de env�o
			_caratula += "" + ";";	//6 N�mero de segmento
			_caratula += "" + ";";	//7 N�mero de Notificaci�n
			_caratula += "" + ";";	//8 C�digo de Autorizaci�n
			_caratula += "\n";

			//RESUMEN
			@SuppressWarnings("rawtypes")
			Iterator it = resumen.entrySet().iterator();
			Map <String, Long> mapResumen = new HashMap<String, Long>();
			while (it.hasNext()) {
                                String factor="";
                                double factor_iva=0;
				mapResumen.clear();
				@SuppressWarnings("rawtypes")
				Map.Entry e = (Map.Entry)it.next();				
				mapResumen = (Map<String, Long>) e.getValue();
				String cant_comun=""+mapResumen.get("CANT_UsoComun");
				String monto_iva_comun=""+mapResumen.get("IVA_UsoComun");
                DecimalFormat df = new DecimalFormat("##.##");
                if(0!=mapResumen.get("IVA_UsoComun")){
                logger.debug("este es el factor"+((double)(mapResumen.get("Monto_UsoComun"))/(double)(mapResumen.get("IVA_UsoComun"))));
                factor=""+df.format(((double)(mapResumen.get("Monto_UsoComun"))/(double)(mapResumen.get("IVA_UsoComun"))));
                }
                String total_iva=""+mapResumen.get("Monto_UsoComun");
				String cant_fijo=""+mapResumen.get("CANT_ActivoFijo");
				String total_fijo=""+mapResumen.get("IVA_ActivoFijo");
				if(cant_comun.equals("0"))
					cant_comun="";
				if(monto_iva_comun.equals("0"))
					monto_iva_comun="";
				if (total_iva.equals("0"))
					total_iva="";
				if (cant_fijo.equals("0"))
					cant_fijo="";
				if (total_fijo.equals("0"))
					total_fijo="";
				
				_resumen += "B" + ";";	//0;
				_resumen += mapResumen.get("TIPO_DOCUMENTO") + ";";	// 1 Tipo de documento
				_resumen += "" + ";";	// 2 null
				_resumen += mapResumen.get("CANTIDAD") + ";";	// 3 Cantidad de documentos
				_resumen += "" + ";";	// 4 Num Operaciones Exentas
				_resumen += mapResumen.get("MONTO_EXENTO") + ";";	// 5 Total exento
				_resumen += mapResumen.get("MONTO_NETO") + ";";	// 6 Total neto
				_resumen += "" + ";";	// 7 null
				_resumen += mapResumen.get("MONTO_IVA") + ";";	// 8 Total IVA
				_resumen += cant_fijo + ";";	// 9 Numero Operaciones IVa Activo Fijo
				_resumen += total_fijo + ";";	// 10 TOTAL MONTO IVA ACTIVO FIJO
				_resumen += cant_comun + ";";	// 11 Num Operaciones IVA Uso Comun
				_resumen += monto_iva_comun + ";";	// 12 Total IVA uso com�n
				_resumen += factor + ";";	// 13 Factor de proporcionalidad del IVA
				_resumen += total_iva.replace(",", ".") + ";";	// 14 Total Cr�dito IVA Uso Com�n
				_resumen += "" + ";";	// 15 Total Ley 18211
				_resumen += "" + ";";	// 16 null
				_resumen += "" + ";";	// 17 Numero de Operaciones con IVA ret total
				_resumen += mapResumen.get("IVA_RetTotal") + ";";	// 18 IVA retenido total
				_resumen += "" + ";";	// 19 Numero de Operaciones con IVA ret parcial
				_resumen += mapResumen.get("IVA_RetParcial") + ";";	// 20 IVA retenido parcial
				_resumen += "" + ";";	// 21 Total cr�dito empresas constructoras
				_resumen += "" + ";";	// 22 Total dep�sitos envases
				_resumen += mapResumen.get("MONTO_TOTAL") + ";";	// 23 Total de totales
				_resumen += "" + ";";	// 24 Total IVA no retenido
				_resumen += "" + ";";	// 25 Total no facturable
				_resumen += "" + ";";	// 26 Total monto per�odo
				_resumen += "" + ";";	// 27 Total venta pasajes nacionales
				_resumen += "" + ";";	// 28 Total venta pasajes internacionales
				_resumen += "" + ";";	// 29 null
				_resumen += "" + ";";	// 30 null
				_resumen += "" + ";";	// 31 null
				_resumen += "" + ";";	// 32 null
				_resumen += mapResumen.get("IVA_Fuera_Plazo") + ";";	// 33 Total Iva Fuera de Plazo
				_resumen += "" + ";";	// 34 Numero de Operaciones Con Iva No Retenido
				_resumen += mapResumen.get("CANT_NULOS") + ";";	// 35 TOTAL ANULADOS
				_resumen += "" + ";";	// 36 null
				_resumen += "" + ";";	// 37 TOTAL IVA propio
				_resumen += "" + ";";	// 38 TOTAL IVA terceros
				_resumen += "\n";	
				if (caratula.get("Tipo_Operacion").equals("COMPRA")) {
                                    logger.debug("este es el alma de papi: " + mapResumen.get("Codigo_IVA_NoRecuperable"));
//				if (mapResumen.get("Codigo_IVA_NoRecuperable")==3L)
//					_resumen += "B2;"+mapResumen.get("Codigo_IVA_NoRecuperable")+";"+mapResumen.get("CANT1")+";" + (mapResumen.get("IVA_NoRecuperable"))+";\n";
//				if (mapResumen.get("Codigo_IVA_NoRecuperable2")==2L)
//					_resumen += "B2;"+mapResumen.get("Codigo_IVA_NoRecuperable2")+";"+mapResumen.get("CANT2")+";" + mapResumen.get("IVA_NoRecuperable2")+";\n";
                                if (mapResumen.get("Codigo_IVA_NoRecuperable")!=0)
					_resumen += "B2;"+mapResumen.get("Codigo_IVA_NoRecuperable")+";"+mapResumen.get("CANT1")+";" + (mapResumen.get("IVA_NoRecuperable"))+";\n";
				}
				if (mapResumen.get("Codigo_Impuesto_Adicional")!=0) {
					
						_resumen += "B1;"+mapResumen.get("Codigo_Impuesto_Adicional")+";"+mapResumen.get("Monto_Impuesto_Adicional")+";;" +";\n";
					
					}
				}

			
			//DETALLE
			for (Map<String, String> map: detalles) {
				if(map.get("Tipo_Documento").equals("35")|| map.get("Tipo_Documento").equals("39")|| map.get("Tipo_Documento").equals("48")){
				}
				else {
				_detalle += "C" + ";";	
				_detalle += map.get("Tipo_Documento") + ";";	// 1 Tipo de documento
				_detalle += map.get("Folio") + ";";	// 2 Folio de documento
				_detalle += map.get("Nulo") + ";";	// 3 Anulado
				_detalle += "" + ";";	// 4 Operaci�n
				_detalle += "" + ";";	// 5 null
				_detalle += "19" + ";";	// 6 Tasa Impuesto
				_detalle += "" + ";";	// 7 N�mero Interno
				_detalle += "" + ";";	// 8 Indicador si corresponde a un servicio per�odico
				_detalle += "" + ";";	// 9 Indicador sin Costo S�lo Facturas
				_detalle += map.get("Fecha_Emision") + ";";	// 10 Fecha documento
				_detalle += "" + ";";	// 11 C�digo de sucursal
				_detalle += map.get("RUT_Contraparte") + ";";	// 12 Rut del contraparte en la operaci�n comerical
				_detalle += map.get("Razon_Social_Contraparte") + ";";	// 13 Raz�n social de la contraparte del documento
				_detalle += "" + ";";	// 14 Tipo de documento de referencia
				_detalle += "" + ";";	// 15 Folio de referencia Folio anulado que ya se ha enviado al SII.
				_detalle += map.get("Monto_Exento") + ";";	// 16 Monto exento
				_detalle += map.get("Monto_Neto") + ";";	// 17 Monto neto
				_detalle += map.get("Monto_IVA") + ";";	// 18 Monto IVA
				_detalle += map.get("IVA_Activo_Fijo") + ";";	// 19 Monto Iva Activo Fijo
				_detalle += map.get("IVA_Uso_Comun") + ";";	// 20 IVA Comun
				_detalle += normalizaCero(map.get("IVA_Fuera_Plazo")) + ";";	// 21 IVA fuera de plazo
				_detalle += normalizaCero(map.get("Ley18211")) + ";";	// 22 Impuesto ley 18211
				_detalle += "" + ";";	// 23 null
				_detalle += normalizaCero(map.get("Monto_IVA_RetenidoTotal")) + ";";	// 24 IVA retenido total
				_detalle += normalizaCero(map.get("Monto_IVA_RetenidoParcial")) + ";";	// 25 IVA retenido parcial
				_detalle += "" + ";";	// 26 Cr�dito 65% empresas
				_detalle += "" + ";";	// 27 Dep�sitos por envases
//				int montotal;
//				int montplazo;
//				montotal = Integer.parseInt(map.get("Monto_Total"));
//				if (!map.get("IVA_NoRecuperable").equals(""))
//					montplazo = Integer.parseInt(map.get("IVA_NoRecuperable"));
//				else 
//					montplazo = 0;
//				montotal = montotal + montplazo;
				_detalle += map.get("Monto_Total")  + ";";	// 28 Monto total del documento
				_detalle += "" + ";";	// 29 IVA no retenido
				_detalle += "" + ";";	// 30 Total monto no facturable
				_detalle += "" + ";";	// 31 Monto del per�odo
				_detalle += "" + ";";	// 32 Venta pasaje nacional
				_detalle += "" + ";";	// 33 Venta pasaje internacional
				_detalle += "" + ";";	// 34 null
				_detalle += "" + ";";	// 35 null
				_detalle += "" + ";";	// 36 null
				_detalle += "" + ";";	// 37 null
				_detalle += "" + ";";	// 38 Excepci�n EMISOR/RECEPTOR
				_detalle += "" + ";";	// 39 null
				_detalle += "" + ";";	// 40 IVA propio
				_detalle += "" + ";";	// 41 IVA terceros
				_detalle += "\n";
				
				//iva no recuperable
				if (caratula.get("Tipo_Operacion").equals("COMPRA")) {
					if (!map.get("Codigo_IVA_NoRecuperable").equals("0"))
						_detalle += "C2;" + map.get("Codigo_IVA_NoRecuperable").replace(".0", "") + ";" + map.get("IVA_NoRecuperable").replace(".0", "") + ";\n" ;
//					if (map.get("Codigo_IVA_NoRecuperable2").equals("2"))
//						_detalle += "C2;" + map.get("Codigo_IVA_NoRecuperable").replace(".0", "") + ";" + map.get("IVA_NoRecuperable").replace(".0", "") + ";\n" ;
//                                        if (map.get("Codigo_IVA_NoRecuperable").equals("1"))
//						_detalle += "C2;" + map.get("Codigo_IVA_NoRecuperable").replace(".0", "") + ";" + map.get("IVA_NoRecuperable").replace(".0", "") + ";\n" ;
					
				}
				
				//impuestos adicionales
				if (map.get("Codigo_Impuesto_Adicional")!=null){
					if (!map.get("Codigo_Impuesto_Adicional").equals("") && !map.get("Codigo_Impuesto_Adicional").equals("null") && !map.get("Codigo_Impuesto_Adicional").equals("0.0")){
					logger.debug("La Chucara " + map.get("Codigo_Impuesto_Adicional"));
						int posicion = map.get("Factor_Impuesto_Adicional").indexOf(".");
						_detalle += "C1;" + map.get("Codigo_Impuesto_Adicional").replace(".0", "") + ";" + map.get("Factor_Impuesto_Adicional").substring(0, posicion+2) + ";" + map.get("Monto_Impuesto_Adicional").replace(".0", "") + ";\n" ;
					}
				}
			}
			}
			
			
		} catch (Exception e) {
			logger.error(e, e);
			throw new Exception("ERROR transformando a TXT: " + e);
		}
		
		return delNULL(_caratula + _resumen + _detalle);
	}

	private String normalizaCero(String value) {
//		logger.debug("-->: " + value);
		if (value == null || value.equals("0"))
			return "";
		return value;
	}

	private String delNULL(String contenido) {
		String result = "";
		result = contenido.replaceAll("null", "");
		
		return result;
	}

	private int getValue(XSSFCell xssfCell) throws Exception {
		
		try {
			String result;
			if (xssfCell == null)
				return 0;
			
			if (xssfCell.getCellType() == 0) {
//				logger.debug("Es numerico");
				return (int) Math.round(xssfCell.getNumericCellValue());
			} else if (xssfCell.getCellType() == 1) {
				logger.debug("Es String");
				result = xssfCell.getStringCellValue();
			} else if (xssfCell.getCellType() == 2) {
				logger.debug("Es formula");
				result = xssfCell.getRawValue();
			} else
				result = xssfCell.getStringCellValue();
			
			if (result.isEmpty())
				result = "0";
			
			return (int) Math.round(Double.parseDouble(result));
		} catch (Exception e) {
			logger.debug(e, e);
			throw new Exception("ERROR obteniendo valor de [" + xssfCell + "]: " + e.getMessage());
		}
		
	}
	
private String getValue2(XSSFCell xssfCell) throws Exception {
		
		try {
			String result;
			if (xssfCell == null)
				return "0";
			
			if (xssfCell.getCellType() == 0) {
				logger.debug("Es numerico");
				double d = xssfCell.getNumericCellValue();
				long lll = (long)d;
				return String.valueOf(lll);
			} else if (xssfCell.getCellType() == 1) {
				logger.debug("Es String");
				result = xssfCell.getStringCellValue();
			} else if (xssfCell.getCellType() == 2) {
				logger.debug("Es formula");
				result = xssfCell.getRawValue();
			} else
				result = xssfCell.getStringCellValue();
			
			if (result.isEmpty())
				result = "0";
			
			return result;
		} catch (Exception e) {
			logger.debug(e, e);
			throw new Exception("ERROR obteniendo valor de [" + xssfCell + "]: " + e.getMessage());
		}
		
	}

	private boolean isDocValido(String tipoDoc, String operacion) throws Exception {
		logger.debug(tipoDoc);
		//IEC 30,32,33,34,40,43,45,46,55,56,60,61,108,901,914,918
		//IEV 30,32,33,34,35,38,39,40,41,43,45,46,55,56,60,61,101,102,103,104,105,106,108,109,110,111,112,901,902,903,919,920,922,924
		//IEV(SOLO RESUMEN)35,38,39,41,105,919,920,922,924

		if (operacion.equals("VENTA")) {
			if ("30,32,33,34,35,38,39,40,41,43,45,46,48,55,56,60,61,101,102,103,104,105,106,108,109,110,111,112,901,902,903,919,920,922,924".contains(tipoDoc))
				return true;
			else
				return false;
		}		
		if (operacion.endsWith("COMPRA")) {
			if ("30,32,33,34,40,43,45,46,48,55,56,60,61,108,901,914,918".contains(tipoDoc))
				return true;
			else
				return false;
		}

		throw new Exception("Tipo_Operacion solo puede ser [VENTA] o [COMPRA].");
	}

	@SuppressWarnings("null")
	private void leeExcel(File fileExcel) throws Exception {
		List<List<XSSFCell>> cellDataList = new ArrayList<List<XSSFCell>>();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileExcel);
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<?> rows = sheet.rowIterator();
            int number=sheet.getLastRowNum();
            System.out.println(" number of rows"+ number);
            
            while (rows.hasNext()) {
                XSSFRow row = ((XSSFRow) rows.next());
                Iterator<Cell> cells = row.cellIterator();
				List<XSSFCell> cellTempList = new ArrayList<XSSFCell>();
				int i= 0;
				for (int j=0; j<row.getLastCellNum(); j++) {
					XSSFCell cell = row.getCell(j);
//					if (cell == null)
//						cell = XSSFCell.CELL_TYPE_STRING;
					cellTempList.add(cell);	
				}
                cellDataList.add(cellTempList);	
             }
        } catch (Exception e) {
        	logger.error(e, e);
           throw new Exception("ERROR leyendo archivo: " + e.getMessage());
        } finally {
            if (fis != null)
                fis.close();
        }
        logger.debug("Cantidad de registros: " + cellDataList.size());
    
        listExcel = cellDataList;
	}

	private void procesaDetalle(List<XSSFCell> rawExcel) throws Exception {
		try {
			//Valida solo la cabecera
			//[Tipo_Documento, Folio, Anulado, Fecha_Emision, RUT_Contraparte, Razon_Social_Contraparte, Monto_Exento, Monto_Neto, Monto_IVA, Monto_IVA_RetenidoTotal, Monto_IVA_RetenidoParcial, Ley18211, Monto_Total,
			//Codigo_IVA_NoRecuperable, IVA_NoRecuperable, Codigo_Impuesto_Adicional, Factor_Impuesto_Adicional, Monto_Impuesto_Adicional]

			if (rawExcel.get(0).toString().equals("Tipo_Documento")) {
				if (!rawExcel.get(1).toString().equals("Folio"))
					throw new Exception("La columna [Folio] fue modificada trasladada.");
				if (!rawExcel.get(2).toString().equals("Nulo"))
					throw new Exception("La columna [Nulo] fue modificada trasladada.");
				if (!rawExcel.get(3).toString().equals("Fecha_Emision"))
					throw new Exception("La columna [Fecha_Emision] fue modificada trasladada.");
				if (!rawExcel.get(4).toString().equals("RUT_Contraparte"))
					throw new Exception("La columna [RUT_Contraparte] fue modificada trasladada.");
				if (!rawExcel.get(5).toString().equals("Razon_Social_Contraparte"))
					throw new Exception("La columna [Razon_Social_Contraparte] fue modificada trasladada.");
				if (!rawExcel.get(6).toString().equals("Monto_Exento"))
					throw new Exception("La columna [Monto_Exento] fue modificada trasladada.");
				if (!rawExcel.get(7).toString().equals("Monto_Neto"))
					throw new Exception("La columna [Monto_Neto] fue modificada trasladada.");
				if (!rawExcel.get(8).toString().equals("Monto_IVA"))
					throw new Exception("La columna [Monto_IVA] fue modificada trasladada.");				
				if (!rawExcel.get(9).toString().equals("Tasa_IVA"))
					throw new Exception("La columna [Tasa_IVA] fue modificada trasladada.");
				if (!rawExcel.get(10).toString().equals("IVA_Fuera_Plazo"))
					throw new Exception("La columna [IVA_Fuera_Plazo] fue modificada trasladada.");				
				if (!rawExcel.get(11).toString().equals("Ley18211"))
					throw new Exception("La columna [Ley18211] fue modificada trasladada.");
				if (!rawExcel.get(12).toString().equals("Monto_Total"))
					throw new Exception("La columna [Monto_Total] fue modificada trasladada.");
				if (!rawExcel.get(13).toString().equals("Monto_IVA_RetenidoTotal"))
					throw new Exception("La columna [Monto_IVA_RetenidoTotal] fue modificada trasladada.");
				if (!rawExcel.get(14).toString().equals("Monto_IVA_RetenidoParcial"))
					throw new Exception("La columna [Monto_IVA_RetenidoParcial] fue modificada trasladada.");
				if (!rawExcel.get(15).toString().equals("Codigo_IVA_NoRecuperable"))
					throw new Exception("La columna [Codigo_IVA_NoRecuperable] fue modificada trasladada.");
				if (!rawExcel.get(16).toString().equals("IVA_NoRecuperable"))
					throw new Exception("La columna [IVA_NoRecuperable] fue modificada trasladada.");
				if (!rawExcel.get(17).toString().equals("Codigo_Impuesto_Adicional"))
					throw new Exception("La columna [Codigo_Impuesto_Adicional] fue modificada trasladada.");
				if (!rawExcel.get(18).toString().equals("Factor_Impuesto_Adicional"))
					throw new Exception("La columna [Factor_Impuesto_Adicional] fue modificada trasladada.");
				if (!rawExcel.get(19).toString().equals("Monto_Impuesto_Adicional"))
					throw new Exception("La columna [Monto_Impuesto_Adicional] fue modificada trasladada.");
						
				return;
			}

			if (!isDocValido(Integer.toString(getValue(rawExcel.get(0))), caratula.get("Tipo_Operacion")))
				throw new Exception("Tipo de Documento " +
						getValue(rawExcel.get(0)) + " no valido para tipo de Informe Electronico " + 
						caratula.get("Tipo_Operacion") + ".");
			
			Map<String, Long> docresumen = new HashMap<String, Long>();
			if (!resumen.containsKey("doc" + getValue(rawExcel.get(0)))) {
				//procede a incorporar tipo de documento en resumen
				docresumen.put("TIPO_DOCUMENTO", (long)getValue(rawExcel.get(0)));
				docresumen.put("CANTIDAD", 0L);
				docresumen.put("CANT_NULOS", 0L);
				docresumen.put("MONTO_EXENTO", 0L);
				docresumen.put("MONTO_NETO", 0L);
				docresumen.put("MONTO_IVA", 0L);
				docresumen.put("MONTO_TOTAL", 0L);
				docresumen.put("CANT1", 0L);
				docresumen.put("CANT2", 0L);
				docresumen.put("CANT_Impuesto_Adicional", 0L);
				docresumen.put("Codigo_Impuesto_Adicional", 0L);
				docresumen.put("Factor_Impuesto_Adicional", 0L);
				docresumen.put("Monto_Impuesto_Adicional", 0L);
				docresumen.put("Codigo_IVA_NoRecuperable", 0L);
				docresumen.put("Codigo_IVA_NoRecuperable2", 0L);
				docresumen.put("IVA_NoRecuperable", 0L);
				docresumen.put("IVA_NoRecuperable2", 0L);
				docresumen.put("IVA_Fuera_Plazo", 0L);
                                docresumen.put("IVA_RetParcial", 0L);
				docresumen.put("IVA_RetTotal", 0L);
                                docresumen.put("IVA_ActivoFijo", 0L);
				docresumen.put("CANT_ActivoFijo", 0L);
                                docresumen.put("IVA_UsoComun", 0L);
				docresumen.put("CANT_UsoComun", 0L);
                                docresumen.put("Monto_UsoComun", 0L);
			} else
				docresumen = resumen.get("doc" + rawExcel.get(0).getRawValue());
			
			//Se procede a acumular datos de RESUMEN
			long cantidad = 1L;
			cantidad = cantidad + docresumen.get("CANTIDAD");
			docresumen.put("CANTIDAD", cantidad);
			

			//CANT_ANULADOS
			if (isAnula(rawExcel.get(2))) {
				long cantAnulados = 1;
				cantAnulados = cantAnulados + docresumen.get("CANT_NULOS");
				docresumen.put("CANT_NULOS", cantAnulados);				
			} else {
				
				
				if (15 < rawExcel.size()){
                                            long codigo = Math.abs(getValue(rawExcel.get(15)));
                                            if (codigo!=0L){
						docresumen.put("Codigo_IVA_NoRecuperable", codigo);
						long cantidadnorecu1 = 1;
						cantidadnorecu1 = cantidadnorecu1 + docresumen.get("CANT1");
						docresumen.put("CANT1", cantidadnorecu1);
						
						long montorecu = Math.abs(getValue(rawExcel.get(16)));
						montorecu = montorecu + docresumen.get("IVA_NoRecuperable");
						docresumen.put("IVA_NoRecuperable", montorecu);
                                            }
//					else if (Math.abs(getValue(rawExcel.get(15)))==2){
//                                            System.out.println("chupa el pico");
//						docresumen.put("Codigo_IVA_NoRecuperable2", 2L);
//						long cantidadnorecu2 = 1;
//						cantidadnorecu2 = cantidadnorecu2 + docresumen.get("CANT2");
//						docresumen.put("CANT2", cantidadnorecu2);
//						System.out.println(docresumen.get("CANT2"));
//						long montorecu2 = Math.abs(getValue(rawExcel.get(16)));
//						montorecu2 = montorecu2 + docresumen.get("IVA_NoRecuperable2");
//						docresumen.put("IVA_NoRecuperable2", montorecu2);
//					}
//                                        else if (Math.abs(getValue(rawExcel.get(15)))==1){
//						docresumen.put("Codigo_IVA_NoRecuperable", 1L);
//						long cantidadnorecu2 = 1;
//						cantidadnorecu2 = cantidadnorecu2 + docresumen.get("CANT1");
//						docresumen.put("CANT1", cantidadnorecu2);
//						
//						long montorecu2 = Math.abs(getValue(rawExcel.get(16)));
//						montorecu2 = montorecu2 + docresumen.get("IVA_NoRecuperable");
//						docresumen.put("IVA_NoRecuperable", montorecu2);
//					}
					
				}
//				else{
//				docresumen.put("Codigo_IVA_NoRecuperable", 7L);
//				docresumen.put("Codigo_IVA_NoRecuperable2", 7L);
//				}
				
				if (17 < rawExcel.size()){
					
						docresumen.put("Codigo_Impuesto_Adicional", (long)Math.abs(getValue(rawExcel.get(17))));
						long cantidadnorecu1 = 1;
						cantidadnorecu1 = cantidadnorecu1 + docresumen.get("CANT_Impuesto_Adicional");
						docresumen.put("CANT_Impuesto_Adicional", cantidadnorecu1);
						
						long montorecu = Math.abs(getValue(rawExcel.get(19)));
						montorecu = montorecu + docresumen.get("Monto_Impuesto_Adicional");
						docresumen.put("Monto_Impuesto_Adicional", montorecu);
					
					
				}
                                
                                if (13 < rawExcel.size()){
					
					//IVA RETENIDO_TOTAL
				long retenidoTotal = Math.abs(getValue(rawExcel.get(13)));
				retenidoTotal = retenidoTotal + docresumen.get("IVA_RetTotal");
				docresumen.put("IVA_RetTotal", retenidoTotal);	
					
					
				}
                                
                                if (14 < rawExcel.size()){
					
					//IVA RETENIDO_PARCIAL
				long retenidoParcial = Math.abs(getValue(rawExcel.get(14)));
				retenidoParcial = retenidoParcial + docresumen.get("IVA_RetParcial");
				docresumen.put("IVA_RetParcial", retenidoParcial);	
					
					
				}
                                
                                 if (20 < rawExcel.size()){
                                    //IVA USO COMUN
                                     if(Math.abs(getValue(rawExcel.get(20)))!=0){
                                    long cantidadusoComun = 1;
                                    cantidadusoComun = cantidadusoComun + docresumen.get("CANT_UsoComun");
                                    docresumen.put("CANT_UsoComun", cantidadusoComun);
                                                
                                    long usoComun = Math.abs(getValue(rawExcel.get(20)));
                                    usoComun = usoComun + docresumen.get("IVA_UsoComun");
                                    docresumen.put("IVA_UsoComun", usoComun);
                                    if(21 < rawExcel.size()){
                                        long usoComun2 = Math.abs(getValue(rawExcel.get(21)));
                                     usoComun2 = usoComun2 + docresumen.get("Monto_UsoComun");
                                     docresumen.put("Monto_UsoComun", usoComun2);
                                    }
                                }
                                 }
                                
                                if (22 < rawExcel.size()){
                                    //IVA ACTIVO FIJO
                                    long cantidadactivoFijo = 1;
                                    cantidadactivoFijo = cantidadactivoFijo + docresumen.get("CANT_ActivoFijo");
                                    docresumen.put("CANT_ActivoFijo", cantidadactivoFijo);
                                                
                                    long activoFijo = Math.abs(getValue(rawExcel.get(22)));
                                    activoFijo = activoFijo + docresumen.get("IVA_ActivoFijo");
                                    docresumen.put("IVA_ActivoFijo", activoFijo);
                                }
				
				//MONTO_EXENTO
				long montoExento = Math.abs(getValue(rawExcel.get(6)));
				montoExento = montoExento + docresumen.get("MONTO_EXENTO");
				docresumen.put("MONTO_EXENTO", montoExento);
				
				//IVA_Fuera_Plazo
				long montoFuera = Math.abs(getValue(rawExcel.get(10)));
				montoFuera = montoFuera + docresumen.get("IVA_Fuera_Plazo");
				docresumen.put("IVA_Fuera_Plazo", montoFuera);
				
				//MONTO_NETO
				long montoNeto = Math.abs(getValue(rawExcel.get(7)));
				montoNeto = montoNeto + docresumen.get("MONTO_NETO");
				docresumen.put("MONTO_NETO", montoNeto);
				
				//MONTO_IVA
				long montoIVA = Math.abs(getValue(rawExcel.get(8)));
				montoIVA = montoIVA	+ docresumen.get("MONTO_IVA");
				docresumen.put("MONTO_IVA", montoIVA);
				
				//MONTO_TOTAL
				long montoTotal = Math.abs(getValue(rawExcel.get(12)));
				montoTotal = montoTotal + docresumen.get("MONTO_TOTAL");
				docresumen.put("MONTO_TOTAL", montoTotal);					
			}
			resumen.put("doc" + rawExcel.get(0).getRawValue(), docresumen);
			
			//Se procede a generar DETALLE
			if (caratula.get("Tipo_Operacion").equals("VENTA") && docSoloResumen(rawExcel.get(0).toString()))
				return;
			//[Tipo_Documento, Folio, Anulado, Fecha_Emision, RUT_Contraparte, Razon_Social_Contraparte, Monto_Exento, Monto_Neto, Monto_IVA, Monto_IVA_RetenidoTotal,
			//  Monto_IVA_RetenidoParcial, Ley18211, Monto_Total, Codigo_IVA_NoRecuperable, IVA_NoRecuperable, Codigo_Impuesto_Adicional, Factor_Impuesto_Adicional, 
			//   Monto_Impuesto_Adicional]
			Map<String, String> det = new HashMap<String, String>();
			det.put("Tipo_Documento", "" + getValue(rawExcel.get(0)));
			det.put("Folio", "" + getValue2(rawExcel.get(1)));
			if (isAnula(rawExcel.get(2))) {
				det.put("Nulo", "A");
				det.put("Fecha_Emision", getFecha(rawExcel.get(3)));
				det.put("RUT_Contraparte", "0-0");
				det.put("Razon_Social_Contraparte", "NULO");
				det.put("Monto_Exento", "" + "0");
				det.put("Monto_Neto", "" + "0");
				det.put("Monto_IVA", "" + "0");			
				det.put("Tasa_IVA", "" + "19");
				det.put("IVA_Fuera_Plazo", "");			
				det.put("Ley18211", "");
				det.put("Monto_Total", "");
				det.put("Monto_IVA_RetenidoTotal", "");
				det.put("Monto_IVA_RetenidoParcial", "");
				det.put("Codigo_IVA_NoRecuperable", "");
				det.put("IVA_NoRecuperable", "");
				det.put("Codigo_Impuesto_Adicional", "");
				det.put("Factor_Impuesto_Adicional", "");
				det.put("Monto_Impuesto_Adicional", "");	
				
			} else {
				det.put("Nulo", "");
				det.put("Fecha_Emision", getFecha(rawExcel.get(3)));
				det.put("RUT_Contraparte", getRUT(rawExcel.get(4).toString().trim()));
				det.put("Razon_Social_Contraparte", getRznSocial(rawExcel.get(5).toString()));
				det.put("Monto_Exento", "" + Math.abs( getValue(rawExcel.get(6))));
				det.put("Monto_Neto", "" + Math.abs(getValue(rawExcel.get(7))));
				det.put("Monto_IVA", "" + Math.abs(getValue(rawExcel.get(8))));	
				det.put("Tasa_IVA", "" + Math.abs(getValue(rawExcel.get(9))));
				det.put("IVA_Fuera_Plazo", "" + Math.abs(getValue(rawExcel.get(10))));
				det.put("Ley18211", "" + Math.abs(getValue(rawExcel.get(11))));
				det.put("Monto_Total", "" + Math.abs(getValue(rawExcel.get(12))));
				if (13 < rawExcel.size())
					det.put("Monto_IVA_RetenidoTotal", "" + Math.abs(getValue(rawExcel.get(13))));
				if (14 < rawExcel.size())
					det.put("Monto_IVA_RetenidoParcial", "" + Math.abs(getValue(rawExcel.get(14))));
				if (15 < rawExcel.size())	
					det.put("Codigo_IVA_NoRecuperable", "" + Math.abs(getValue(rawExcel.get(15))));
				else
					det.put("Codigo_IVA_NoRecuperable", "0");
//				if (16 < rawExcel.size())
//					det.put("Codigo_Impuesto_Adicional", "" + Math.abs(getValue(rawExcel.get(16))));
				if (16 < rawExcel.size())
					det.put("IVA_NoRecuperable", "" + Math.abs(getValue(rawExcel.get(16))));
				else 
					det.put("IVA_NoRecuperable", "0");
				if (17 < rawExcel.size())
					det.put("Codigo_Impuesto_Adicional", "" + getFracion(rawExcel.get(17)));
				if (18 < rawExcel.size())
					det.put("Factor_Impuesto_Adicional", "" + getFracion(rawExcel.get(18)));
				if (19 < rawExcel.size())
					det.put("Monto_Impuesto_Adicional", "" + Math.abs(getValue(rawExcel.get(19))));
				if (20 < rawExcel.size()){
					logger.debug("Iva Uso Comun Detalle: "+ Math.abs(getValue(rawExcel.get(20))));
					det.put("IVA_Uso_Comun", "" + Math.abs(getValue(rawExcel.get(20))));
			}
				if (21 < rawExcel.size())
					det.put("IVA_Proporcional", "" + Math.abs(getValue(rawExcel.get(21))));
				if (22 < rawExcel.size())
					det.put("IVA_Activo_Fijo", "" + Math.abs(getValue(rawExcel.get(22))));
			}
			detalles.add(det);
			return;
			
		} catch (Exception e) {
			logger.error(e, e);
			throw new Exception("ERROR procesando DETALLE " + rawExcel + ": " + e.getMessage());
		}
	}

	private String getFecha(XSSFCell xssfCell) {
		String result = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if (xssfCell.getCellType() == 0)
			result = (sdf.format(xssfCell.getDateCellValue()));
        else
        	result = (xssfCell.getStringCellValue());	
		return result;
	}

	private String getFracion(XSSFCell xssfCell) {
		Double result;
		if (xssfCell == null)
			return "";
		else
			result = xssfCell.getNumericCellValue();
		return Double.toString(result);
	}

	private boolean isAnula(XSSFCell xssfCell) {
		String result;
		if (xssfCell == null)
			result =  "";
		else
			result = xssfCell.toString();
		
		if (result.toLowerCase().contains("anula"))
			return true;
		return false;
	}

	private void procesaEncabezado(List<XSSFCell> rawExcel) throws Exception {
		try {
			if (rawExcel.size() == 1)
				throw new Exception("ERROR no se definio dato de caracter obligatorio: " + rawExcel);
			
			if (rawExcel.get(0).toString().equals("RUT_Emisor")) {
				caratula.put("RUT_Emisor", getRUT(rawExcel.get(1).toString()));
				return;
			}

			if (rawExcel.get(0).toString().equals("Periodo")) {
				String periodo = "";
				if (rawExcel.get(1).getCellType() == 0) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
					periodo  = sdf.format(rawExcel.get(1).getDateCellValue());
				} else 
					periodo = rawExcel.get(1).toString();
				
				caratula.put("Periodo", periodo);
				return;
			}

			if (rawExcel.get(0).toString().equals("Tipo_Operacion")) {
				if (rawExcel.get(1).toString().toUpperCase().equals("VENTA"))
					caratula.put("Tipo_Operacion", "VENTA");
				else if (rawExcel.get(1).toString().toUpperCase().equals("COMPRA"))
					caratula.put("Tipo_Operacion", "COMPRA");
				else
					throw new Exception("Tipo operacion solo puede ser [VENTA] o [COMPRA]");
				
				
				return;
			}
			
		}catch (Exception e) {
			throw new Exception("ERROR procesando Encabezado " + rawExcel + " : " + e);
		}
	}

	private void procesaResumen(List<XSSFCell> rawExcel) throws Exception {
		try {
			//Valida solo la cabecera
			if (rawExcel.get(0).toString().equals("TIPO_DOCUMENTO")) {
				if (!rawExcel.get(1).toString().equals("CANTIDAD"))
					throw new Exception("La columna [CANTIDAD] fue modificada trasladada.");
				if (!rawExcel.get(2).toString().equals("CANT_NULOS"))
					throw new Exception("La columna [CANT_NULOS] fue modificada trasladada.");
				if (!rawExcel.get(6).toString().equals("MONTO_EXENTO"))
					throw new Exception("La columna [MONTO_EXENTO] fue modificada trasladada.");
				if (!rawExcel.get(7).toString().equals("MONTO_NETO"))
					throw new Exception("La columna [MONTO_NETO] fue modificada trasladada.");
				if (!rawExcel.get(8).toString().equals("MONTO_IVA"))
					throw new Exception("La columna [MONTO_IVA] fue modificada trasladada.");
				if (!rawExcel.get(12).toString().equals("MONTO_TOTAL"))
					throw new Exception("La columna [MONTO_TOTAL] fue modificada trasladada.");
				
				 resumen = new HashMap<String, Map<String, Long>>();				
				return;
			}
			
			//Valida la data
			if (!isDocValido(Integer.toString(getValue(rawExcel.get(0))), caratula.get("Tipo_Operacion")))
				throw new Exception("Tipo documento [" +
						getValue(rawExcel.get(0)) + "] no es valido para tipo de Operacion [" +
						caratula.get("Tipo_Operacion") + "]");
			
			Map<String, Integer> docresumen = new HashMap<String, Integer>();
//			if (!resumen.containsKey("doc" + rawExcel.get(0).getRawValue())) {		
//				docresumen.put("TIPO_DOCUMENTO", getValue(rawExcel.get(0)));
//				docresumen.put("CANTIDAD", getValue(rawExcel.get(1)));
//				docresumen.put("CANT_NULOS", getValue(rawExcel.get(2)));
//				docresumen.put("CANT_USO_COMUN", getValue(rawExcel.get(3)));
//				docresumen.put("CANT_ACTIVO_FIJO", getValue(rawExcel.get(4)));
//				docresumen.put("MONTO_ACTIVO_FIJO", getValue(rawExcel.get(5)));
//				docresumen.put("MONTO_EXENTO", getValue(rawExcel.get(6)));
//				docresumen.put("MONTO_NETO", getValue(rawExcel.get(7)));
//				docresumen.put("MONTO_IVA", getValue(rawExcel.get(8)));
//				docresumen.put("MONTO_IVA_COMUN", getValue(rawExcel.get(9)));
//				docresumen.put("FACTOR_IVA", getValue(rawExcel.get(10)));
//				proporcionalidad = getValue2(rawExcel.get(10));
//				logger.debug("Chupalo con mayo" +getValue2(rawExcel.get(10)));
//				docresumen.put("TOTAL_IVA", getValue(rawExcel.get(11)));
//				docresumen.put("MONTO_TOTAL", getValue(rawExcel.get(12)));
//				if (13 < rawExcel.size())
//					docresumen.put("CANT1", getValue(rawExcel.get(13)));
//				else
//					docresumen.put("CANT1", 0);
//				if (14 < rawExcel.size())
//					docresumen.put("CANT2", getValue(rawExcel.get(14)));
//				else
//					docresumen.put("CANT2", 0);
//				if (15 < rawExcel.size())	
//					docresumen.put("Codigo_IVA_NoRecuperable", getValue(rawExcel.get(15)));
//				else
//					docresumen.put("Codigo_IVA_NoRecuperable", 0);
//				if (16 < rawExcel.size())
//					docresumen.put("IVA_NoRecuperable", getValue(rawExcel.get(16)));
//				else
//					docresumen.put("IVA_NoRecuperable", 0);
//				if (17 < rawExcel.size())
//					docresumen.put("Codigo_IVA_NoRecuperable2", getValue(rawExcel.get(17)));
//				else
//					docresumen.put("Codigo_IVA_NoRecuperable2", 0);
//				if (18 < rawExcel.size())
//					docresumen.put("IVA_NoRecuperable2", getValue(rawExcel.get(18)));
//				else
//					docresumen.put("IVA_NoRecuperable2", 0);
//			} else
//				throw new Exception("No pueden existir Tipo de Documentos repetidos en RESUMEN.");
//			Map<String, String> docresumen2 = new HashMap<String, String>();
//			if (!resumen.containsKey("doc" + rawExcel.get(0).getRawValue())) {		
//				docresumen2.put("FACTOR_IVA", getValue2(rawExcel.get(10)));
//			} else
//				throw new Exception("No pueden existir Tipo de Documentos repetidos en RESUMEN.");
//			
			//resumen.put("doc" + rawExcel.get(0).toString(), docresumen);
			
//			Map <String,Integer> mapaux = new HashMap<String,Integer>();
//			Iterator it = docresumen.entrySet().iterator();
//			while (it.hasNext()) {
//				Map.Entry e = (Map.Entry)it.next();
//				mapaux.put(e.getKey().toString(), (Integer) e.getValue());
//				
//			};
//			logger.debug("key: " + rawExcel.get(0).getRawValue());
//			resumen.put("doc" + rawExcel.get(0).getRawValue(), mapaux);
//			docresumen.clear();
//			
		} catch (Exception e) {
			throw new Exception("ERROR procesando RESUMEN " + rawExcel + " :" + e.getMessage());
		}
		
	}

}
