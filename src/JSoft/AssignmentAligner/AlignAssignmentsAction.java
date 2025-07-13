/*
************************************************
    Author	:   Jeevan Kumar
    Company	:   J Soft
    Created on	:   13-Jul-2025  7:00:25 pm
    
    Created With MicroSoft Co-Pilot's Help!
    
************************************************
 */
package JSoft.AssignmentAligner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;
import org.netbeans.api.editor.EditorRegistry;
import javax.swing.text.JTextComponent;

@ActionID(
	category = "Edit",
	id = "JSoft.AssignmentAligner.AlignAssignmentsAction"
)
@ActionRegistration(
	iconBase = "JSoft/AssignmentAligner/AssignmentAlignerIcon.png",
	displayName = "#CTL_AlignAssignmentsAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Source", position = 9100, separatorBefore = 9050),
    @ActionReference(path = "Shortcuts", name = "AS-A")
})
@Messages("CTL_AlignAssignmentsAction=Align Assignments")
public final class AlignAssignmentsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
	// TODO implement action body
	JTextComponent editor = EditorRegistry.lastFocusedComponent();
	if (editor == null || editor.getSelectionStart() == editor.getSelectionEnd()) {
	    return;
	}
	StyledDocument doc = (StyledDocument) editor.getDocument(); // Cast to StyledDocument
	int start = editor.getSelectionStart();
	int end = editor.getSelectionEnd();

	try {
	    String selectedText = doc.getText(start, end - start);
	    String[] lines = selectedText.split("\n");

	    List<String[]> splitLines = new ArrayList<>();
	    int maxLhs = 0;
	    int maxRhs = 0;

	    boolean inBlockComment = false;
	    List<String> blockCommentBuffer = new ArrayList<>();
	    String leadingIndentForBlock = "";

	    for (int i = 0; i < lines.length; i++) {
		String line = lines[i];
		line = line.replaceAll("[^\\x00-\\x7F]", ""); //  Sanitize here
		String trimmed = line.trim();

		if (inBlockComment) {
		    blockCommentBuffer.add(line);
		    if (trimmed.contains("*/")) {
			inBlockComment = false;
			splitLines.add(new String[]{"BLOCK_COMMENT", leadingIndentForBlock, String.join("\n", blockCommentBuffer)});
			blockCommentBuffer.clear();
		    }
		    continue;
		}

		if (line.contains("=")) {
		    String leadingWhitespace = line.substring(0, line.indexOf(line.trim()));

		    String comment = "";
		    String codePart = line;

		    int blockIndex = line.indexOf("/*");
		    int lineIndex = line.indexOf("//");

		    if (blockIndex >= 0 && (lineIndex == -1 || blockIndex < lineIndex)) {
			if (!line.contains("*/")) {
			    // Start of multiline comment
			    inBlockComment = true;
			    leadingIndentForBlock = leadingWhitespace;
			    blockCommentBuffer.add(line.substring(blockIndex).trim());
			    codePart = line.substring(0, blockIndex).trim();
			} else {
			    comment = line.substring(blockIndex).trim();
			    codePart = line.substring(0, blockIndex).trim();
			}
		    } else if (lineIndex >= 0) {
			comment = line.substring(lineIndex).trim();
			codePart = line.substring(0, lineIndex).trim();
		    }

		    String[] assignmentSplit = codePart.split("=", 2);
		    if (assignmentSplit.length < 2) {
			splitLines.add(new String[]{line});
			continue;
		    }

		    String lhs = assignmentSplit[0].trim();
		    String rhs = assignmentSplit[1].replace(";", "").trim();

		    splitLines.add(new String[]{leadingWhitespace, lhs, rhs, comment});
		    maxLhs = Math.max(maxLhs, lhs.length());
		    maxRhs = Math.max(maxRhs, rhs.length());
		} else {
		    splitLines.add(new String[]{line});
		}
	    }

	    // this verision advanced; aligns the single line comment and multiline after the semi colon.
	    List<String> formattedLines = new ArrayList<>();
	    for (String[] parts : splitLines) {
		if (parts.length == 4) {
		    String indent = parts[0];
		    String paddedLhs = String.format("%-" + maxLhs + "s", parts[1]);

		    //  Attach semicolon to RHS before padding
		    String rhsWithSemicolon = parts[2] + ";";
		    String paddedRhs = rhsWithSemicolon;
		    if (rhsWithSemicolon.length() < maxRhs + 1) {
			paddedRhs = String.format("%-" + (maxRhs + 1) + "s", rhsWithSemicolon);
		    }

		    String commentPad = parts[3].isEmpty() ? "" : "   " + parts[3];

		    formattedLines.add(indent + paddedLhs + " = " + paddedRhs + commentPad);

		} else if (parts.length == 3 && "BLOCK_COMMENT".equals(parts[0])) {
		    String indent = parts[1];
		    String[] commentLines = parts[2].split("\n");

		    // Align all lines of the block comment to the comment column
		    for (int j = 0; j < commentLines.length; j++) {
			String cleanComment = commentLines[j].replaceAll("[^\\x00-\\x7F]", ""); //  Sanitize
			formattedLines.add(indent + String.format("%-" + (maxLhs + maxRhs + 6) + "s", "") + cleanComment);
		    }

		} else {
		    formattedLines.add(parts[0]);
		}
	    }

	    String finalText = String.join("\n", formattedLines);
	    finalText = finalText.replaceAll("[^\\x00-\\x7F]", "").trim();

	    doc.remove(start, end - start);
	    doc.insertString(start, finalText, null);

	} catch (BadLocationException ex) {
	    Exceptions.printStackTrace(ex);
	}

    }
}
