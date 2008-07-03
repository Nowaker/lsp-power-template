package nu.staldal.zt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.transform.Result;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nu.staldal.xmltree.Element;
import nu.staldal.xmltree.Node;
import nu.staldal.lsp.wrapper.BooleanArrayCollection;
import nu.staldal.lsp.wrapper.DoubleArrayCollection;
import nu.staldal.lsp.wrapper.FloatArrayCollection;
import nu.staldal.lsp.wrapper.IntArrayCollection;
import nu.staldal.lsp.wrapper.LongArrayCollection;
import nu.staldal.lsp.wrapper.ReadonlyBeanMap;
import nu.staldal.lsp.wrapper.ResourceBundleTuple;
import nu.staldal.lsp.wrapper.ShortArrayCollection;
import nu.staldal.util.Environment;

/**
 * Element with ZeroTemplate commands.
 *
 * @author Mikael St�ldal
 */
public class ZtElement extends Element {

    private static final long serialVersionUID = -8783416221544659625L;
    
    private final List<ZtAttr> attrs;
    private final String string;
    private final boolean stringIsLiteral;
    private final String map;
    private final String list;    
    
    public ZtElement(Element element, List<ZtAttr> attrs, String string,
            boolean stringIsLiteral, String map, String list) {
        super(element);
        this.attrs = attrs;
        this.string = string;
        this.stringIsLiteral = stringIsLiteral;
        this.map = map;
        this.list = list;
    }

    public List<ZtAttr> getAttrs() {
        return attrs;
    }

    public String getString() {
        return string;
    }

    public boolean isStringIsLiteral() {
        return stringIsLiteral;
    }

    public String getMap() {
        return map;
    }

    public String getList() {
        return list;
    }

    protected String getString(String key, Environment<String,Object> env) 
            throws SAXException {
        if (!env.containsKey(key)) {
            throw new SAXException("Parameter " + key + " not defined");
        }
        Object value = env.lookup(key);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String)value;            
        } else if (value instanceof CharSequence) {            
            return value.toString(); // TODO handle CharSequence directly?
        } else if (value instanceof char[]) {
            return new String((char[])value);
        } else if (value instanceof Enum) {
            return value.toString();    
        } else {
            throw new SAXException("Parameter " + key + " must be string, but is: " + value.getClass().getName());                
        }        
    }
    
    protected String getStringNotNull(String key, Environment<String,Object> env) 
            throws SAXException {
        String value = getString(key, env);
        if (value == null) {
            throw new SAXException("Parameter " + key + " is null");                
        }
        return value;
    }
    
    protected boolean getBooleanNotNull(String key, Environment<String,Object> env) 
            throws SAXException {
        if (!env.containsKey(key)) {
            throw new SAXException("Parameter " + key + " not defined");
        }
        Object value = env.lookup(key);
        if (value == null) {
            throw new SAXException("Parameter " + key + " is null");
        } else if (value instanceof Boolean) {
            return (Boolean)value;
        } else {
            throw new SAXException("Parameter " + key + " must be boolean, but is: " + value.getClass().getName());                
        }        
    }
    
    protected Map<String,Object> getMapNotNull(String key, Environment<String,Object> env) 
            throws SAXException {
        if (!env.containsKey(key)) {
            throw new SAXException("Parameter " + key + " not defined");
        }
        Object value = env.lookup(key);
        if (value == null) {
            throw new SAXException("Parameter " + key + " is null");
        } else if (value instanceof Map) {
            return (Map<String,Object>)value;
        } else if (value instanceof ResourceBundle) {
            return new ResourceBundleTuple((ResourceBundle)value);        
        } else if (value instanceof CharSequence
                    || value instanceof char[]
                    || value instanceof Enum
                    || value instanceof Boolean) {
            throw new SAXException("Parameter " + key + " must be map, but is: " + value.getClass().getName());                            
        } else {
            return new ReadonlyBeanMap(value);
        }        
    }
    
    protected Iterable<?> getListNotNull(String key, Environment<String,Object> env)
            throws SAXException {
        if (!env.containsKey(key)) {
            throw new SAXException("Parameter " + key + " not defined");
        }
        Object value = env.lookup(key);
        if (value == null) {
            throw new SAXException("Parameter " + key + " is null");
        } else if (value instanceof Iterable) {            
            return (Iterable<?>)value;
        } else if (value instanceof int[]) {
            return new IntArrayCollection((int[])value);
        } else if (value instanceof short[]) {
            return new ShortArrayCollection((short[])value);
        } else if (value instanceof long[]) {
            return new LongArrayCollection((long[])value);
        } else if (value instanceof float[]) {
            return new FloatArrayCollection((float[])value);
        } else if (value instanceof double[]) {
            return new DoubleArrayCollection((double[])value);
        } else if (value instanceof boolean[]) {
            return new BooleanArrayCollection((boolean[])value);
        } else if (value instanceof Object[]) {
            Object[] arr = (Object[])value;
            if (arr.length == 0)
                return Collections.emptyList();
            else
                return Arrays.asList(arr);
        } else {
            throw new SAXException("Parameter " + key + " must be list, but is: " + value.getClass().getName());                
        }        
    }
    
    public void toSAX(ContentHandler sax, Environment<String, Object> env) 
            throws SAXException {
        if (map != null) {
            Map<String,Object> value = getMapNotNull(map, env);
            env.pushFrame(value);
        }
        
        if (list != null) {
            Iterable<?> value = getListNotNull(list, env);
            for (Object item : value) {
                if (item instanceof CharSequence
                        || item instanceof char[]
                        || item instanceof Enum
                        || item instanceof Boolean) {
                    env.pushFrame();                    
                    env.bind("", item);
                } else if (item instanceof Map) {
                    env.pushFrame((Map<String,Object>)item);
                } else if (item instanceof ResourceBundle) {
                    env.pushFrame(new ResourceBundleTuple((ResourceBundle)item));
                } else {
                    env.pushFrame(new ReadonlyBeanMap(item));                    
                }
                _toSAX(sax, env);                
                env.popFrame();
            }
        } else {
            _toSAX(sax, env);
        }
        
        if (map != null) {
            env.popFrame();
        }
    }
    
    private void _toSAX(ContentHandler sax, Environment<String, Object> env) 
            throws SAXException {
        if (!attrs.isEmpty()) {
            Map<String,String> attrCopy = new HashMap<String,String>(getAttributes());
            
            for (ZtAttr ztAttr : attrs) {
                String value = getString(ztAttr.getString(), env);
                if (value == null) {
                    attrCopy.remove(ztAttr.getAttributeName());
                } else {
                    attrCopy.put(ztAttr.getAttributeName(), value);
                }
            }
            
            outputStartElement(sax, attrCopy);                        
        } else {
            outputStartElement(sax);            
        }
        
        if (string != null) {
            String value = getStringNotNull(string, env);
            if (stringIsLiteral) {
                sax.processingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "");
            }
            sax.characters(value.toCharArray(), 0, value.length());
            if (stringIsLiteral) {
                sax.processingInstruction(Result.PI_ENABLE_OUTPUT_ESCAPING, "");
            }
        } else {
            for (Node node : this) {
                if (node instanceof ZtElement) {
                    ((ZtElement)node).toSAX(sax, env);
                } else {
                    node.toSAX(sax);
                }
            }
        }
        
        outputEndElement(sax);
    }
    
}
