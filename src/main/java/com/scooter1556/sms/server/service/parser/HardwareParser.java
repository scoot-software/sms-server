package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.domain.GraphicsCard;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HardwareParser {
    
    private static final String CLASS_NAME = "HardwareParser";
    
    public static GraphicsCard[] getGraphicsCards() {
        String parser = ParserUtils.getHardwareParser();
       
        if(parser == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Unable to parse graphics card information.", null);
            return null;
        }
        
        try {
            String[] command = new String[]{parser, "-class", "video", "-xml"};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
                        
            //Parse XML
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(process.getInputStream());
                        
            // Root node
            NodeList root = doc.getChildNodes();
            Node list = ParserUtils.getNode("list", root);
                        
            // Check root node is present
            if(list == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unexpected output while parsing graphics cards.", null);
                return null;
            }
            
            // Check list node has child nodes
            if(!list.hasChildNodes()) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "No graphics card information found.", null);
                return null;
            }
            
            // Get nodes
            NodeList nodes = list.getChildNodes();
            
            List<GraphicsCard> graphicsCards = new ArrayList<>();
            
            for(int n = 0; n < nodes.getLength(); n++) {
                Node node = nodes.item(n);
                NodeList elements = node.getChildNodes();
                
                // Check if the GPU is enabled
                boolean enabled = Boolean.parseBoolean(ParserUtils.getNodeAttr("claimed", node, "false"));
                
                if(!enabled) {
                    continue;
                }
                
                // Vendor and model
                String vendor = ParserUtils.getNodeValue("vendor", elements, "");
                String product = ParserUtils.getNodeValue("product", elements, "");
                
                if(vendor.isEmpty() || product.isEmpty()) {
                    continue;
                }
                
                // Create new graphics card instance
                GraphicsCard graphicsCard = new GraphicsCard();
                graphicsCard.setVendor(vendor);
                graphicsCard.setProduct(product);
                
                // Bus info
                String sBusInfo = ParserUtils.getNodeValue("businfo", elements, "");
                
                if(!sBusInfo.isEmpty()) {
                    String[] busInfo = sBusInfo.split("@");
                    
                    if(busInfo.length > 1) {
                        graphicsCard.setId(busInfo[1]);
                    }
                }
                
                // Configuration node
                Node config = ParserUtils.getNode("configuration", elements);
                
                if(config != null && config.hasChildNodes()) {
                    NodeList settings = config.getChildNodes();
                    
                    for(int s = 0; s < settings.getLength(); s++) {
                        Node setting = settings.item(s);
                        
                        if(ParserUtils.getNodeAttr("id", setting, "").equals("driver")) {
                            graphicsCard.setDriver(ParserUtils.getNodeAttr("value", setting, ""));
                            break;
                        }
                    }
                }
                
                //  Add to array
                graphicsCards.add(graphicsCard);
            }
            
            if(graphicsCards.isEmpty()) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No graphics cards found.", null);
                return null;
            }
            
            return graphicsCards.toArray(new GraphicsCard[graphicsCards.size()]);
        } catch (Exception x) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Parsing of graphics cards failed.", x);
            return null;
        }
    }
}
