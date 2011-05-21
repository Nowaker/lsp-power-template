package nu.staldal.lsp;

import java.io.File;

public class LspTestConstants {

		public static final File LSP_DIR = new File(
			LspTestConstants.class.getClassLoader().getResource("").getFile() + "lspPages");

		public static final File ZT_DIR = new File(
			LspTestConstants.class.getClassLoader().getResource("").getFile() + "ztPages");

}
