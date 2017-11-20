/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peran.dependency.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import de.peran.dependency.analysis.data.ClazzChangeData;

/**
 * Helps to compare whether two versions of a file may have changed performance (and whether this change is for the use of the whole file or only some methods).
 * @author reichelt
 *
 */
public final class FileComparisonUtil {
	
	private static final Logger LOG = LogManager.getLogger(FileComparisonUtil.class);

	/**
	 * Util class should not be initialized
	 */
	private FileComparisonUtil(){
		
	}
	
	public static void clearComments(final Node node) {
		final Optional<Comment> comment = node.getComment();
		if (comment.isPresent()) {
			final Comment realComment = comment.get();
			if (realComment.getComment() != null) {
				realComment.setContent("");
			}
			node.setComment(null);
		}
		node.getOrphanComments().clear();
		final List<Node> childNodes = node.getChildNodes();
		LOG.trace("Type: {}", childNodes.getClass());
		for (final Iterator<Node> begin = childNodes.iterator(); begin.hasNext();) {
			final Node child = begin.next();
			if (child instanceof LineComment) {
				((LineComment) child).setContent("");
			} else if (child instanceof JavadocComment){
				((JavadocComment) child).setContent("");
			}else {
				clearComments(child);
			}
		}
	}

	/**
	 * Compares two nodes and returns eventually happened changes
	 * @param node1 First node for comparison
	 * @param node2 Second node for comparison
	 * @return Eventually empty List of changed Nodes
	 */
	public static List<Node> comparePairwise(final Node node1, final Node node2) {
		final List<Node> changes = new LinkedList<>();

		final List<Node> childs1 = cleanUnneccessary(node1.getChildNodes());
		final List<Node> childs2 = cleanUnneccessary(node2.getChildNodes());
		
		if (node1.getChildNodes().size() != node2.getChildNodes().size() && childs1.size() != childs2.size()) {
			LOG.info("Size of change: " + node1.hashCode() + "(" + node1.getChildNodes().size() + ") " + node2.hashCode() + "(" + node2.getChildNodes().size() + ") ");
			changes.add(node1);
			changes.add(node2);
		} else {
			final Iterator<Node> node2iterator = childs2.iterator();
			for (final Iterator<Node> node1iterator = childs1.iterator(); node1iterator.hasNext();) {
				final Node child = node1iterator.next();
				final Node child2 = node2iterator.next();
				if (child.getClass() != child2.getClass()) {
					changes.add(child);
					changes.add(child2);
				} else {

					if (!child.equals(child2)) {
						if (child instanceof BlockStmt || child2 instanceof BlockStmt) {
							changes.add(child);
							changes.add(child2);
						}

						/*
						 * Currently, nothing is done in this case, as a declaration change should cause an call change (which will be an BlockStmt-Change), or the method is not called, than the
						 * declaration change will not be relevant. TODO Is it detected, if the e.g. a public static ClassA asd = new ClassA(1) instead of new ClassA(2)? (cause this is a declaration
						 * as well)
						 */

						changes.addAll(comparePairwise(child, child2));
					} else {
						LOG.trace("Equal: {} {}", child , child2);
					}

				}
			}
		}

		return changes;
	}
	
	private static boolean isNodeNotRelevant(final Node testNode){
		return testNode instanceof ImportDeclaration || testNode instanceof LineComment || testNode instanceof JavadocComment;
	}

	private static List<Node> cleanUnneccessary(final List<Node> childs1) {
		final List<Node> result = new LinkedList<>();
		result.addAll(childs1);
		for (final Iterator<Node> iterator = result.iterator(); iterator.hasNext(); ){
			final Node testNode = iterator.next();
			if (isNodeNotRelevant(testNode)){
				iterator.remove();
			}
		}
		return result;
	}

	/**
	 * Returns the information whether the source has changed
	 * @param newFile	Old File to check
	 * @param oldFile	New File to check
	 * @return Changedata, i.e. if a change happened and if it was class- or method-wide
	 * @throws ParseException If Class can't be parsed
	 * @throws IOException If class can't be read
	 */
	public static ClazzChangeData getChangedMethods(final File newFile, final File oldFile) throws ParseException, IOException {
		final CompilationUnit newCu = JavaParser.parse(newFile);
		final CompilationUnit oldCu = JavaParser.parse(oldFile);

		final ClazzChangeData changedata = new ClazzChangeData(newFile.getName());
		try {
			clearComments(newCu);
			clearComments(oldCu);
			
			final List<Node> changes = comparePairwise(newCu, oldCu);

			if (changes.size() == 0) {
				changedata.setChange(false);
				return changedata;
			}
			
			boolean onlyLineCommentOrImportChanges = true;
			for (final Node node : changes) {
				if (!(node instanceof LineComment) && !(node instanceof ImportDeclaration)){
					onlyLineCommentOrImportChanges = false;
				}
			}
			if (onlyLineCommentOrImportChanges){
				return changedata;
			}

			for (final Node node : changes) {
				if (node instanceof Statement) {
					final Statement statement = (Statement) node;
					Node parent = statement.getParentNode().get();
					boolean finished = false;
					while (!finished && parent.getParentNode() != null) {
						if (parent instanceof ConstructorDeclaration) {
							changedata.getChangedMethods().add("<init>");
							finished = true;
						}
						if (parent instanceof MethodDeclaration) {
							final MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
							changedata.getChangedMethods().add(methodDeclaration.getNameAsString());
							finished = true;
						}
						parent = parent.getParentNode().get();
					}
					if (!finished) {
						changedata.setOnlyMethodChange(false);
					}
				} else if (node instanceof ClassOrInterfaceDeclaration) {
					changedata.setOnlyMethodChange(false);
				} else {
					changedata.setOnlyMethodChange(false);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			LOG.info("Found full-class change");
			changedata.setOnlyMethodChange(false);
		}

		return changedata;
	}
}
