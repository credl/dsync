package datastructures;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MultiProperties extends Properties{
	private static final long serialVersionUID = -7968176034110626042L;

	public MultiProperties(){	
	}
	
	public MultiProperties(Properties prop){
		super(prop);
	}
	
	public void load(InputStream inStream) throws IOException{
		super.load(inStream);
		decapsulateMultiset();
	}

	public void load(Reader reader) throws IOException{
		super.load(reader);
		decapsulateMultiset();
	}

	public void loadFromXML(InputStream in) throws IOException{
		super.load(in);
		decapsulateMultiset();
	}

	@SuppressWarnings("deprecation")
	public void save(OutputStream out, String comments){ 
		encapsulateMultiset();
		super.save(out, comments);
		decapsulateMultiset();
	}

	public void store(OutputStream out, String comments) throws IOException { 
		encapsulateMultiset(false);
		super.store(out, comments);
		/*
		encapsulateMultiset(true);
		PrintWriter pw = new PrintWriter(out);
		for (Object key : keySet()){
			Object value = get(key);
			pw.println(key.toString() + "=" + value.toString());
		}
		pw.close();
		*/
		decapsulateMultiset();
	}

	public void store(Writer writer, String comments) throws IOException { 
		encapsulateMultiset();
		super.store(writer, comments);
		decapsulateMultiset();
	}
	public void storeToXML(OutputStream os, String comment) throws IOException { 
		encapsulateMultiset();
		super.storeToXML(os, comment);
		decapsulateMultiset();
	}
	
	public void storeToXML(OutputStream os, String comment, String encoding) throws IOException{ 
		encapsulateMultiset();
		super.storeToXML(os, comment, encoding);
		decapsulateMultiset();
	}

	@SuppressWarnings("unchecked")
	private void encapsulateMultiset(){
		encapsulateMultiset(false);
	}
	@SuppressWarnings("unchecked")
	private void encapsulateMultiset(boolean incluenewlnie){
		for (Object k : keySet()){
			String key = (String)k;
			Object val = get(key);
			if (val instanceof List){
				String newvalue = "";
				List<String> list = (List<String>)val;
				boolean firstelement = true;
				for (String element : list){
					// Quote all existing semicolons since ; is now used for value separation
					newvalue = newvalue + (!firstelement ? ";" + (incluenewlnie ? "\\\n" : "") : "") + element.replace(";", "\\;");
					firstelement = false;
				}
				put(key, newvalue);
			}
		}
	}
	
	private void decapsulateMultiset(){
		for (Object k : keySet()){
			String key = (String)k;
			String[] values = getProperty(key).split(";");
			List<String> list = new ArrayList<String>();
			String currentvalue = "";
			for (int i = 0; i < values.length; i++){
				currentvalue = currentvalue + values[i];
				// Check if the ; was quoted
				boolean semicolonquoted = false;
				for (int j = currentvalue.length() - 1; j >= 0 && currentvalue.charAt(j) == '\\'; j--){
					semicolonquoted = !semicolonquoted;
				}
				
				// If the ; was quoted, it was not a real value separator
				if (semicolonquoted){
					currentvalue = currentvalue + ";";
				}else{
					list.add(currentvalue);
					currentvalue = "";
				}
			}
			put(key, list);
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> getAsList(String key){
		Object o = get(key);
		if (o == null) return null;
		if (o instanceof List){
			return (List<String>)o;
		}else{
			List<String> l = new ArrayList<String>();
			l.add(o.toString());
			return l;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String getProperty(String key){
		Object o = get(key);
		if (o == null) return null;
		if (o instanceof List && ((List<String>)o).size() > 0){
			return ((List<String>)o).get(0);
		}else{
			return o.toString();
		}
	}
}