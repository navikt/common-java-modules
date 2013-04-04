package no.nav.sbl.dialogarena.pdf;

import org.apache.commons.collections15.Transformer;
import java.awt.Dimension;

abstract class ConvertToPng implements Transformer<byte[], byte[]> {

    public Dimension frameDimension;

    public ConvertToPng(Dimension frameDimension) {
        this.frameDimension = frameDimension;
    }
}
