package cl.facele.docele.transformer.logica;

/**
 * Esta clase es una implementacion del RUT usado en Chile
 * 
 */
public class RUT {

	private int[] numero;
	private char digito;

	public final short RUT_LENGTH = 9;

	/**
	 * Crea un nuevo RUT vacio
	 */
	public RUT() {
		numero = new int[RUT_LENGTH];
	}

	/**
	 * Genera un rut a partir de un string en formato xxxxxxxx-x o formato
	 * xx.xxx.xxx-x - Elimina los ceros del principio, por si vienen (jourzua)
	 * 
	 * @param rut
	 *            El String representativo del rut
	 * @throws Exception
	 *             En caso que el String del RUT no esta bien formado o el
	 *             digito verificador no corresponda
	 */
	public RUT(String prut) throws Exception {
		this();
		String rut = "";
		
		if (prut != null)
			rut = prut;
		else
			throw new Exception("RUT incorrecto. Valor es 'NULL'");

		char dig = rut.charAt(rut.length() - 2);
		if (dig != '-')
			throw (new Exception(
					"El RUT no esta bien formado, no se encontro el '-' que separa al digito ["
							+ prut + "]"));
		dig = rut.charAt(rut.length() - 1);

		if (!Character.isDigit(dig) && Character.toUpperCase(dig) != 'K')
			throw (new Exception(
					"El RUT no esta bien formado, si no es digito debe ser K ["
							+ prut + "]"));

		digito = Character.toUpperCase(dig);

		rut = rut.substring(0, rut.length() - 2);

		int sl = rut.length();

		if (sl > RUT_LENGTH + (RUT_LENGTH / 3))
			throw (new Exception(
					"La cantidad de digitos del RUT no es valido [" + prut
							+ "]"));

		int j = 0;
		for (int i = 0; i < sl; i++) {
			char ch = rut.charAt(sl - i - 1);
			if (j != 0 && (j % 3) == 0 && ch == '.')
				continue;
			if (!Character.isDigit(ch))
				throw (new Exception("El RUT no esta bien formado ["
						+ prut + "]"));
			numero[RUT_LENGTH - 1 - j++] = Character.digit(ch,
					Character.LETTER_NUMBER);
		}

		if (Character.toUpperCase(digito) != getDigito())
			throw (new Exception("El digito verificador no corresponde:"
					+ digito + " <> " + getDigito() + "[RUT: " + prut + "]"));
	}

	/**
	 * Obtiene el RUT en formato xx.xxx.xxx-x
	 * 
	 * @return El String del RUT
	 */
	public String getFormated() {
		String value = new String();
		boolean escribir = false;
		int point = 0;
		for (int i = 0; i < RUT_LENGTH; i++) {
			if (numero[i] != 0)
				escribir = true;
			if (point != 0 && (point % 3) == 0)
				value += ".";
			if (escribir)
				value += Character.toUpperCase(Character.forDigit(numero[i],
						Character.LETTER_NUMBER));
			point++;
		}
		return (value + "-" + digito);

	}

	/**
	 * Obtiene el RUT en formato xxxxxxxx-x
	 * 
	 * @return El String del RUT
	 */
	@Override
	public String toString() {
		String value = new String();
		boolean escribir = false;
		for (int i = 0; i < RUT_LENGTH; i++) {
			if (numero[i] != 0)
				escribir = true;
			if (escribir)
				value += Character.toUpperCase(Character.forDigit(numero[i],
						Character.LETTER_NUMBER));
		}
		return (value + "-" + digito);
	}

	/**
	 * Obtiene el RUT del contribuyente sin puntos, guiones ni digito
	 * verificador
	 * 
	 * @return el RUT del contribuyente sin puntos, guiones ni digito
	 *         verificador
	 */
	public String getPure() {
		String value = new String();
		boolean escribir = false;
		for (int i = 0; i < RUT_LENGTH; i++) {
			if (numero[i] != 0)
				escribir = true;
			if (escribir)
				value += Character.toUpperCase(Character.forDigit(numero[i],
						Character.LETTER_NUMBER));
		}
		return (value);
	}

	/**
	 * Calcula el digito verificador del RUT
	 * 
	 * @return El digito verificador
	 */
	public char getDigito() {
		int mult = 2;
		int suma = 0;

		for (int i = numero.length - 1; i >= 0; i--) {
			suma += mult * numero[i];
			if (mult == 7)
				mult = 2;
			else
				mult++;
		}
		suma = suma % 11;
		switch (suma) {
		case 0:
			return ('0');
		case 1:
			return ('K');
		default:
			return (Character.toUpperCase(Character.forDigit(11 - suma,
					Character.LETTER_NUMBER)));
		}
	}

	/**
	 * Calcula el digito verificador del RUT
	 * 
	 * @return El digito verificador
	 */
	public String getSDigito() {
		return ((new Character(getDigito())).toString());
	}

	/**
	 * Compara que dos RUTs son iguales
	 * 
	 * @param obj
	 *            El RUT contra el cual se desea comprar
	 * @return true en caso que sean iguales o false en caso contrario
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RUT))
			return (false);
		RUT rut = (RUT) obj;
		if (rut.digito != digito)
			return (false);
		for (int i = 0; i < RUT_LENGTH; i++)
			if (rut.numero[i] != numero[i])
				return (false);
		return (true);
	}

}
