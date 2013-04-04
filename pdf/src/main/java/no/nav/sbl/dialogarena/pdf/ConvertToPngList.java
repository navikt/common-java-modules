package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import java.awt.Dimension;
import java.util.List;

abstract class ConvertToPngList implements Transformer<byte[], List<byte[]>> {

    public Dimension frameDimension;

    public ConvertToPngList(Dimension frameDimension) {
        this.frameDimension = frameDimension;
    }
}
