// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.Projection;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

@BasicPreferences(true)
@BasicWiremock
@Projection
class WMSEndpointTileSourceTest implements TileSourceTest {
    @BasicWiremock
    WireMockServer tileServer;

    private void basicMock() {
        final byte[] response = assertDoesNotThrow(() -> Files.readAllBytes(
                Paths.get(TestUtils.getTestDataRoot() + "wms/geofabrik-osm-inspector.xml")));
        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/capabilities?SERVICE=WMS&REQUEST=GetCapabilities"))
                        .willReturn(WireMock.aResponse().withBody(response))
        );
    }

    @Override
    public ImageryInfo getInfo() {
        this.basicMock();
        final ImageryInfo info = new ImageryInfo("WMSEndpointTileSourceTest");
        info.setExtendedUrl(tileServer.url("/capabilities"));
        info.setDefaultLayers(Collections.singletonList(new DefaultLayer(ImageryType.WMS_ENDPOINT,
                "single_node_in_way", "default", null)));
        info.setImageryType(ImageryType.WMS_ENDPOINT);
        return info;
    }

    @Override
    public TemplatedTileSource getTileSource(ImageryInfo info) {
        return new WMSEndpointTileSource(info, ProjectionRegistry.getProjection());
    }

    @Test
    void testDefaultLayerSetInMaps() throws Exception {

        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/capabilities?SERVICE=WMS&REQUEST=GetCapabilities"))
                .willReturn(
                        WireMock.aResponse()
                        .withBody(Files.readAllBytes(Paths.get(TestUtils.getTestDataRoot() + "wms/geofabrik-osm-inspector.xml")))
                        )
                );

        tileServer.stubFor(WireMock.get(WireMock.urlEqualTo("/other/maps")).willReturn(WireMock.aResponse().withBody(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<imagery xmlns=\"http://josm.openstreetmap.de/maps-1.0\">\n" +
                "<entry>\n" +
                "<name>OSM Inspector: Geometry</name>\n" +
                "<id>OSM_Inspector-Geometry</id>\n" +
                "<type>wms_endpoint</type>\n" +
                "<category>qa</category>\n" +
                "<url><![CDATA[" + tileServer.url("/capabilities") + "]]></url>\n" +
                "<icon>data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAAsSAAALEgHS3X78AAAB5UlEQVQ4y4WTwWsTURDG" +
                "fy8W1yYmXZOqtGJJFyGw6KF7CEigwYuS0kthrYUi4i0iORS9BU9hQdA/ILcixVBrwENKLz1FUBB0wWOwYFAqxUNYTZq6BfM8yC5d05iBObz3vfnmm3kz4sqDh/zP" +
                "7szdlG5I+Of1zQ1xFA8xxI4GH2cjg4Cl+UUJcC4SJq6c7FPkKRlIoPQk0+NnuDwxHrhvuYd83+8OVuBlHouE/eDXzW8+/qO9DyHB0vyiVHoy2INSNiPdeg23XuPs" +
                "3icmIoofPKXGmFJjjEUjgf4EFNi2TT6fJ5FI0Gg0ePrkMRfnbvn41QsJgEAJAQUdbYZyuQxAcvoSpmnydesFAF+cn8f2KUCw/fGt6GgzWJbF706bVCoFwGxyktnk" +
                "5N8kB79QepL1zQ3xbOulCJWyGbkQHZWlbEZ6JIZhBDI1nQ5Np8P2zi4t9zAwGyNe3QALti11XSedTvsPYrEY73f3Bk+irusAnI6qrNy7z43sNUbFCQC6LYdCoYBb" +
                "r/k1/2sh690HUalUaH7eIRxXA+6RFItF3HqN6+dP9REIb5lK2Yy0bdsHDMMgl8vRbTkAhOMqlmVhmibLq2ui7xsf1d+IV+0D3zVNw7KsPiXVapXnd2/Lodu4vLom" +
                "TNMcSvIHY6bDkqJtEqIAAAAASUVORK5CYII=</icon>\n" +
                "<attribution-text mandatory=\"true\">© Geofabrik GmbH, OpenStreetMap contributors, CC-BY-SA</attribution-text>\n" +
                "<attribution-url>http://tools.geofabrik.de/osmi/</attribution-url>\n" +
                "<max-zoom>18</max-zoom>\n" +
                "<valid-georeference>true</valid-georeference>\n" +
                "<default-layers>" +
                "<layer name=\"single_node_in_way\" style=\"default\" />" +
                "</default-layers>" +
                "</entry>\n" +
                "</imagery>"
                )));

        Config.getPref().putList("imagery.layers.sites", Collections.singletonList(tileServer.url("/other/maps")));
        ImageryLayerInfo.instance.loadDefaults(true, null, false);
        assertEquals(1, ImageryLayerInfo.instance.getDefaultLayers().size());
        ImageryInfo wmsImageryInfo = ImageryLayerInfo.instance.getDefaultLayers().get(0);
        assertEquals("single_node_in_way", wmsImageryInfo.getDefaultLayers().get(0).getLayerName());
        WMSEndpointTileSource tileSource = new WMSEndpointTileSource(wmsImageryInfo, ProjectionRegistry.getProjection());
        tileSource.initProjection(Projections.getProjectionByCode("EPSG:3857"));
        assertEquals("https://tools.geofabrik.de/osmi/views/geometry/wxs?FORMAT=image/png&TRANSPARENT=TRUE&VERSION=1.3.0&SERVICE=WMS&"
                + "REQUEST=GetMap&LAYERS=single_node_in_way&STYLES=default&"
                + "CRS=EPSG:3857&WIDTH=512&HEIGHT=512&"
                + "BBOX=20037508.3427893,-60112525.0283678,60112525.0283678,-20037508.3427893", tileSource.getTileUrl(1, 1, 1));
    }

    @Test
    void testCustomHeadersServerSide() throws IOException {
        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/capabilities?SERVICE=WMS&REQUEST=GetCapabilities"))
                .willReturn(
                        WireMock.aResponse()
                        .withBody(Files.readAllBytes(Paths.get(TestUtils.getTestDataRoot() + "wms/webatlas.no.xml")))
                        )
                );

        tileServer.stubFor(WireMock.get(WireMock.urlEqualTo("/other/maps")).willReturn(WireMock.aResponse().withBody(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<imagery xmlns=\"http://josm.openstreetmap.de/maps-1.0\">\n" +
                "  <entry>\n" +
                "        <name>Norway Orthophoto (historic)</name>\n" +
                "        <name lang=\"nb\">Norge i Bilder (historisk)</name>\n" +
                "        <id>geovekst-nib-historic</id>\n" +
                "        <type>wms_endpoint</type>\n" +
                "        <category>photo</category>\n" +
                "        <country-code>NO</country-code>\n" +
                "        <description lang=\"en\">Historic Norwegian orthophotos and maps, courtesy of Geovekst and Norkart.</description>\n" +
                "        <url><![CDATA[" + tileServer.url("/capabilities?SERVICE=WMS&REQUEST=GetCapabilities") + "]]></url>\n" +
                "        <custom-http-header header-name=\"X-WAAPI-TOKEN\" header-value=\"b8e36d51-119a-423b-b156-d744d54123d5\" />\n" +
                "        <attribution-text>© Geovekst</attribution-text>\n" +
                "        <attribution-url>https://www.norgeibilder.no/</attribution-url>\n" +
                "        <permission-ref>https://forum.openstreetmap.org/viewtopic.php?id=62083</permission-ref>\n" +
                "        <icon>https://register.geonorge.no/data/organizations/_L_norgeibilder96x96.png</icon>\n" +
                "        <max-zoom>21</max-zoom>\n" +
                "        <valid-georeference>true</valid-georeference>\n" +
                "</entry>\n" +
                "</imagery>"
                )));

        Config.getPref().putList("imagery.layers.sites", Collections.singletonList(tileServer.url("/other/maps")));
        ImageryLayerInfo.instance.loadDefaults(true, null, false);
        ImageryInfo wmsImageryInfo = ImageryLayerInfo.instance.getDefaultLayers().get(0);
        wmsImageryInfo.setDefaultLayers(Collections.singletonList(new DefaultLayer(ImageryType.WMS_ENDPOINT, "historiske-ortofoto", "", "")));
        WMSEndpointTileSource tileSource = new WMSEndpointTileSource(wmsImageryInfo, ProjectionRegistry.getProjection());
        tileSource.initProjection(Projections.getProjectionByCode("EPSG:3857"));
        assertEquals("b8e36d51-119a-423b-b156-d744d54123d5", wmsImageryInfo.getCustomHttpHeaders().get("X-WAAPI-TOKEN"));
        assertEquals("b8e36d51-119a-423b-b156-d744d54123d5", tileSource.getHeaders().get("X-WAAPI-TOKEN"));
        assertTrue(wmsImageryInfo.isGeoreferenceValid());
        tileServer.verify(
                WireMock.getRequestedFor(WireMock.urlEqualTo("/capabilities?SERVICE=WMS&REQUEST=GetCapabilities"))
                .withHeader("X-WAAPI-TOKEN", WireMock.equalTo("b8e36d51-119a-423b-b156-d744d54123d5")));
        assertEquals("http://waapi.webatlas.no/wms-orto-hist/?"
                + "FORMAT=image/png&"
                + "TRANSPARENT=TRUE&"
                + "VERSION=1.1.1&"
                + "SERVICE=WMS&"
                + "REQUEST=GetMap&"
                + "LAYERS=historiske-ortofoto&"
                + "STYLES=&"
                + "SRS=EPSG:3857&"
                + "WIDTH=512&"
                + "HEIGHT=512&"
                + "BBOX=20037508.3427893,-60112525.0283678,60112525.0283678,-20037508.3427893",
                tileSource.getTileUrl(1, 1, 1));
    }
}
