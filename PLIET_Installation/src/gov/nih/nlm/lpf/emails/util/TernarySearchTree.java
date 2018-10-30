/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nih.nlm.lpf.emails.util;

import java.util.Vector;
import java.io.*;


/**
 * This class implements the Ternary Search Tree described in the paper 
 * "Fast Algorithms for Sorting and Searching Strings" of Jon L. Bentley and Robert Sedegewick, 
 * which was presented at Eighth Annual ACM-SIAM Symposium on Discrete Algorithms, New Orleans, January, 1997 
 * 
 * @ Change Log:  Adopted from an initial version
 */
public class TernarySearchTree implements Serializable
{
        
    /* *******************************************************************************************
    * Each node of the tree is stored in an instance of TreeNode class.
    * 'splitChar', the split character of the node; the coming character is compared with 'splitChar' 
    * and go down to the three branches: low kid, high kid and equal kid consequently. 
     * 
    * When splitChar is equal to 0, it represents the ending of a string like the '0' in the char array in C programing.
    * The string is stored at the 'storeWord' only when it is a leaf node, i.e., 'splitChar' is equal to 0. 
    * 'freqCount' makes sense only when the node is a leaf node, and it is used as the counter
     * of the word frequency.
    ***************************************************************************************************/ 
    private class TreeNode implements Serializable
    {
        char splitChar;
        int ind_lowKid = -1;
        int ind_highKid = -1;
        int ind_equalKid = -1;
        int ind_parent = -1;
        int freqCount = -1;

        boolean isRemoved = false;
    }
    
    
    
    Vector nodePool = new Vector();  
    
    
    // Create an empty tree to which nodes will be inserted later
    public TernarySearchTree()
    {
        initPool();
    }
    
    // Instantiate a tree from the specified lexicon
    public TernarySearchTree(String filename, boolean recordFrequency)
    {
        initPool();
        if (recordFrequency)
            generateTreeFromWordList2(filename);        // for entries with word and frequency
        else
            generateTreeFromWordList(filename);          // for single word (entry) records
    }
    
 /***************************************************************************************************/   
    // This method reads a serialized binary lexicon object from an input file
    // Currently not used - due to slow speed --- check (dm)
/***************************************************************************************************/
    public TernarySearchTree loadTree(String fileName)
    {
        TernarySearchTree aTree = new TernarySearchTree();
        try
        {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(fileName));
            aTree = (TernarySearchTree)inputStream.readObject();
            inputStream.close();
     
        }
        catch(Exception e){
            System.out.println("Exception occurred in reading file:" + e);
            e.printStackTrace();
        }
        return aTree;
    }
       
  
    protected  void cleanup()
    {
        nodePool.clear();
    }
    
    // clean the node pool and put the root node into the pool. 
    protected  void initPool()
    {
        cleanup();
        TreeNode root = new TreeNode();
        root.splitChar = 0;
        root.ind_equalKid = root.ind_highKid = root.ind_lowKid = -1;
        nodePool.add(root);         // empty root node
    }
    
    /***************************************************************************
     *  insert a word into the dictionary
     ****************************************************************************/
    public void insert(String aWord)
    {
        insert(0, aWord, aWord, -1);
        return;
    }
    
    /* insert a word under the branch of the node indexed by 'indOfNode', where
     * 'aStr' is the string running in the recursive program,
     * 'aWord' is the original word.
     * The 'insert()' starts from the root node, compares the current node with
     * the current character in the string being stored, 
     * goes to the branches of its low kid, equal kid and high kid recursively, 
     * and finally add the string character-by-character into the dictionary. 
     * The 1st large 'if' branch adds a new node at the end of the pool when the
     * index of input node is out of the tree
     * The 2nd, 3rd and 4th 'if' branch goes to the low kid, equal kid and high kid of current node . 
     * recursively. The insert function will do nothing to the tree if the word being inserted has existed.
     */ 
    public int insert(int indOfNode, String aStr, String aWord, int indOfParent)
    {
        TreeNode currentNode;
        char currentChar;
        
        // When aStr is empty string "", current Character is taken as 0 like the C Program
        if (aStr.length() > 0) currentChar = aStr.charAt(0);
        else currentChar = 0;
        
        //add an element to the node pool when index is negative - happens during recursion
        if (indOfNode < 0)
        {
            indOfNode = nodePool.size()-1 + 1;
            TreeNode aNode = new TreeNode();
            aNode.splitChar = currentChar;
            aNode.freqCount = 1;
            aNode.ind_equalKid = aNode.ind_highKid = aNode.ind_lowKid = -1;
            aNode.ind_parent = indOfParent;
            //if (aStr.equals("")) aNode.storedWord = aWord; // store the word when it goes to the last element of this string
            nodePool.add(aNode);
        }
        currentNode = (TreeNode)nodePool.elementAt(indOfNode);
        if (currentChar < currentNode.splitChar)
        {
            currentNode.ind_lowKid = insert(currentNode.ind_lowKid, aStr, aWord, indOfNode); 
            nodePool.set(indOfNode, currentNode);
        }
        if (currentChar == currentNode.splitChar) 
        {
            if (currentChar!=0) // if 'currentChar' is equal to 0, then the word exisits.
            {
                // parameter'aStr' (the 'pointer' of aWord) only move forward in the branch of equal kid
                currentNode.ind_equalKid = insert(currentNode.ind_equalKid, aStr.substring(1), aWord, indOfNode); 
                nodePool.set(indOfNode, currentNode);
            }
            else
            {
                if (currentNode.isRemoved)
                {
                    currentNode.isRemoved = false; 
                    currentNode.freqCount = 1;
                }
                else 
                    currentNode.freqCount++;
                nodePool.set(indOfNode, currentNode);
            }
        }   
        if (currentChar > currentNode.splitChar) 
        {
            currentNode.ind_highKid = insert(currentNode.ind_highKid, aStr, aWord, indOfNode); 
            nodePool.set(indOfNode, currentNode);
        }
        return indOfNode; 
              
    }
    
    /*
     * remove a word from the dictionary if it exisits.
     * since the limitation of the data structure, I simply mark the removed word 
     * So pay attention here: to trim the tree physically, you must update the tree.
     */
    
    public int remove(String aWord)
    {
        int indOfNode = this.search(aWord);
        if (indOfNode < 0) return  -1;
        else
        {
            TreeNode currentNode = (TreeNode)nodePool.elementAt(indOfNode);
            currentNode.isRemoved = true;
            nodePool.set(indOfNode, currentNode);
        }   
        return 0;
    }
    // get appearance frequency of a word
    public int getFrequency(int indOfNode)
    {
        TreeNode nd = (TreeNode)nodePool.elementAt(indOfNode);
        return nd.freqCount;
    }
    public int getFrequency(String w)
    {
        int ind = search(w);
        int cnt = getFrequency(ind);
        
        // get lower case
        String lw = w.toLowerCase();
        if (!lw.equals(w)) ind = search(lw);
        else ind = search(w.substring(0,1).toUpperCase() + w.substring(1));
        if (ind>=0) cnt = cnt + getFrequency(ind);
        // get singular format freq
        // should add new property for n., v. and so on
        if (lw.endsWith("s") && lw.length()>3)
        {
            ind = search(lw.substring(0,lw.length()-1));
            if (ind>=0) cnt = cnt + getFrequency(ind);
        }
        
        return cnt;
    }
    
    public String getWord(int indOfNode)
    {
        //sth. wrong with assert(indOfNode<0...)
        assert(indOfNode<0 && indOfNode<nodePool.size());
        String rWord = "";
        TreeNode currentNode = (TreeNode)nodePool.elementAt(indOfNode);
        TreeNode parentNode;
        while(currentNode.ind_parent >= 0)
        {
            parentNode = (TreeNode)nodePool.elementAt(currentNode.ind_parent);
            if (parentNode.ind_equalKid == indOfNode) // only read the equal kids
            {
                rWord = String.valueOf(parentNode.splitChar) + rWord;
            }
            indOfNode = currentNode.ind_parent;
            currentNode = parentNode;
        }
        
        return rWord;
    }
    
    /* search a word in the dictionary 
     * return index of the word in the dictionary when it is found, -1 otherwise
     * search(), pmsearch() and nearsearch() follow the similar recursive logic as insert().
     */
    public int search(String aWord)
    { 
        if (nodePool.size()==0) return 0;
        
        int indOfNode = 0; // index of current node
        int indOfChar = 0; // index of current character of string being searched, which works like a pointer in C
        TreeNode currentNode;  
        char currentChar;
        while(indOfNode >= 0)
        {
            currentNode = (TreeNode) nodePool.elementAt(indOfNode);
            if (indOfChar<aWord.length()) 
                currentChar = aWord.charAt(indOfChar);
            else 
                currentChar = 0;
            
            if (currentNode.splitChar > currentChar) 
                indOfNode = currentNode.ind_lowKid;
            else if (currentNode.splitChar < currentChar) 
                indOfNode = currentNode.ind_highKid;
            else
            {
                if (currentNode.splitChar == currentChar) 
                {
                    if (currentChar==0) if (!currentNode.isRemoved) return indOfNode; 
                }  
                indOfNode = currentNode.ind_equalKid;
                indOfChar++;
            }
        }
        return -1;
    }
    
    /* search a "partial match" word in the dictionary,
     * where '.' could be used as a wild card
     * for example, input 'CHAR.E' could return the output 'CHARGE', 'CHARBE' if they exisit in the dictionary 
     */
    public String[] pmsearch(String aStr)
    {  
        Vector sv = pmsearch(0, aStr);
        String[] words = new String[sv.size()]; 
        sv.toArray(words);
        return words;
    }
     /*public String[] pmsearch(String aStr)
    {  
        String[] words = null;
        Vector sv = pmsearch(0, aStr);
        if (sv.size()>0) 
        {
            words = new String[sv.size()];
            sv.toArray(words);
        } 
        return words;
    }*/
    
    public Vector pmsearch(int indOfNode, String aStr)
    {
        Vector result = new Vector();
        if (indOfNode<0) return result;
        Vector sv1, sv2, sv3; // the result from 3 branches
        TreeNode currentNode = (TreeNode)nodePool.elementAt(indOfNode);
        char currentChar;
        if (aStr.length()>0) 
            currentChar = aStr.charAt(0);
        else 
            currentChar = 0;
        
        //if current Character is '.' the wild card then we should search all the 3 branches of current node
        if (currentChar=='.' || currentNode.splitChar>currentChar)
        { 
            sv1 = pmsearch(currentNode.ind_lowKid, aStr); result.addAll(sv1);
        }
        if (currentChar=='.' || currentNode.splitChar<currentChar) 
        { 
            sv2 = pmsearch(currentNode.ind_highKid, aStr); result.addAll(sv2);
        }
        if (currentChar=='.' || currentNode.splitChar==currentChar) 
        { 
            if (currentNode.splitChar!=0 && currentChar!=0) 
            { 
                sv3 = pmsearch(currentNode.ind_equalKid, aStr.substring(1));
                result.addAll(sv3);
            }
        }
        if (currentNode.splitChar==0 && currentChar==0 && !currentNode.isRemoved) 
            //result.add(currentNode.storedWord);
            result.add(getWord(indOfNode));
        return result;
    }
    
    // search the words within given hamming distance from the given word
    // 'd' is the given hamming distance
    public String[] nearsearch(String aWord, int d)
    {
        Vector sv = nearsearch(0, aWord, d);
        String[] words = new String[sv.size()];
        sv.toArray(words);
        return words;
    }
    
    public Vector nearsearch(int indOfNode, String aStr, int d) 
    {
        Vector result = new Vector();
        
        // return empty vector if we are running out of the tree or 'd' is negative
        if (indOfNode<0 || d < 0) return result; 
        
        Vector sv1, sv2, sv3;
        TreeNode currentNode = (TreeNode)nodePool.elementAt(indOfNode);
        char currentChar;
        
        if (aStr.length()>0) 
            currentChar = aStr.charAt(0);
        else 
            currentChar=0;
        
        // we should search all the branches of current node, if 'd' is bigger than 0
        if (d > 0 || currentChar < currentNode.splitChar) 
        {
            sv1 = nearsearch(currentNode.ind_lowKid, aStr, d); 
            result.addAll(sv1);
        }
        if (d > 0 || currentChar > currentNode.splitChar) 
        {
            sv2 = nearsearch(currentNode.ind_highKid, aStr, d); 
            result.addAll(sv2);
        }
        if (currentNode.splitChar==0)
        { 
            // exclude the removed word and the empty string, i.e. the word at the root
            if (aStr.length()<=d && !currentNode.isRemoved && currentNode.ind_parent>=0) 
                //result.add(currentNode.storedWord)
                result.add(getWord(indOfNode));
        }
        else 
        {
            // we should go to the equal kid as long as 'splitChar' is not zero
            // the pointer of the string should move forward if 'aStr' is not empty
            // the distance 'd' should reduce by 1 if current character is NOT equal to the split character
            sv3 = nearsearch(currentNode.ind_equalKid, (currentChar==0 ? aStr:aStr.substring(1)),
                        (currentChar==currentNode.splitChar ? d:d-1 ));
            result.addAll(sv3);
        }
        
        return result;
    }
    
    // Search the words within the edit distance "d" from  the reference word.
    // edit diatance indicates the max. number of operations required to
    // convert the given word to a  word in the ternary tree (i.e. a lexicon word)
    public String[] editSearch(String aWord, int d)
    {
        Vector sv = editSearch(0, aWord, d);
        String[] words = new String[sv.size()];
        sv.toArray(words);
        return words;
    }
    
    
    public Vector editSearch(int indOfNode, String aStr, int d) 
    {
        Vector result = new Vector();
        
        // return empty vector if we are running out of the tree or 'd' is negative
        if (indOfNode<0 || d < 0) return result; 
        
        Vector sv1, sv2, sv3, sv4, sv5;
        TreeNode currentNode = (TreeNode)nodePool.elementAt(indOfNode);
        char currentChar;
        
        if (aStr.length()>0) 
            currentChar = aStr.charAt(0);
        else 
            currentChar=0;
        
        // we should search all the branches of current node, if 'd' is bigger than 0
        if (d > 0 || currentChar < currentNode.splitChar) 
        {
            sv1 = editSearch(currentNode.ind_lowKid, aStr, d); 
            result.addAll(sv1);
        }
        if (d > 0 || currentChar > currentNode.splitChar) 
        {
            sv2 = editSearch(currentNode.ind_highKid, aStr, d); 
            result.addAll(sv2);
        }
        if (currentNode.splitChar==0)
        { 
            // exclude the removed word and the empty string, i.e. the word at the root
            if (aStr.length()<=d && !currentNode.isRemoved && currentNode.ind_parent>=0) 
                //result.add(currentNode.storedWord)
                result.add(getWord(indOfNode));
        }
        else 
        {
            // we should go to the equal kid as long as 'splitChar' is not zero
            // the pointer of the string should move forward if 'aStr' is not empty
            // the distance 'd' should reduce by 1 if current character is NOT equal to the split character
            sv3 = editSearch(currentNode.ind_equalKid, (currentChar==0 ? aStr:aStr.substring(1)), 
                (currentChar==currentNode.splitChar ? d:d-1) );
            // additional edit search
            if (currentChar!=0 && currentChar!=currentNode.splitChar)
            {
                sv4 = editSearch(currentNode.ind_equalKid, aStr, d-1); // corresponds to insertion to search word
                sv5 = editSearch(indOfNode, aStr.substring(1), d-1); // corresponds to deletion to search word
            }
            result.addAll(sv3);
        }
        
        return result;
    }
    
    // this search method is to return the search results that match any (partial) word in the input
    // the result is equivalent to search the words individually and combine the results together
    // however, I would like to re-organize the input words so that some overlapped search works are avoided
    public String[] pmsearchInGroup(String[] pfs)
    {
        String[] rs = null;
        TernarySearchTree pfsTree = new TernarySearchTree();
        for(int i=0;i<pfs.length;i++) pfsTree.insert(pfs[i]);
        
        Vector rv = pmsearchInGroup(0, 0, pfsTree);
        rs = new String[rv.size()];
        rv.toArray(rs);
        
        return rs;
    }
    
    public Vector pmsearchInGroup(int indOfNode, int indOfpfsNode, TernarySearchTree pfsTree)
    {
        Vector results = new Vector();
        if(indOfNode<0 || indOfpfsNode<0) return results;
        
        TreeNode currentNode = (TreeNode)nodePool.elementAt(indOfNode);
        TreeNode currentpfsNode = (TreeNode)pfsTree.nodePool.elementAt(indOfpfsNode);
        Vector sv;
        
        if(currentNode.splitChar == currentpfsNode.splitChar || currentpfsNode.splitChar == '.') 
        {
            if(currentNode.splitChar !=0 && currentpfsNode.splitChar !=0)
            {
                sv = pmsearchInGroup(currentNode.ind_equalKid, currentpfsNode.ind_equalKid, pfsTree);
                results.addAll(sv);
            }
        }
        if(currentNode.splitChar==0 && currentpfsNode.splitChar==0 && indOfNode!=0 && !currentNode.isRemoved)
        {
            results.add(getWord(indOfNode));
        }
        if(currentNode.splitChar>currentpfsNode.splitChar || currentpfsNode.splitChar=='.')
        {
            sv = pmsearchInGroup(currentNode.ind_lowKid, indOfpfsNode, pfsTree);
            results.addAll(sv);
        }
        if(currentNode.splitChar<currentpfsNode.splitChar || currentpfsNode.splitChar=='.')
        {
            sv = pmsearchInGroup(currentNode.ind_highKid, indOfpfsNode, pfsTree);
            results.addAll(sv);
        }
        
        sv = pmsearchInGroup(indOfNode, currentpfsNode.ind_lowKid, pfsTree);
        results.addAll(sv);
        
        sv = pmsearchInGroup(indOfNode, currentpfsNode.ind_highKid, pfsTree);
        results.addAll(sv);
        
        return results;
    }
   
/**********************************************************************************/
     // input:  file containing words one word per line, 
    // If frequency is recordrd, simply ignore that
    public int  generateTreeFromWordList(String inputFile)
    {
        int status = 1;
        try
        {   
            // read the word list into a ternary tree
             BufferedReader inputStream = new BufferedReader(new FileReader(inputFile));
             String aWord = inputStream.readLine();
             while(aWord != null)
             {
                 String[] words = aWord.trim().split("\\s+");
                 insert(words[0]);               // ignore multiple-words
                 aWord = inputStream.readLine();
             }                                   
        }
        catch(Exception e)
        {
            System.out.println("Exception occurred in writing file:" + e);
            e.printStackTrace();
            status = 0;
        }
        return status;
    }
   
 
   /***********************************************************************************/     
    // input:  file containing words and their frequency, one word per line 
    //
    public int  generateTreeFromWordList2(String inputFile)
    {
        int status = 1;
        try
        {   
            // read the word list into a ternary tree
             BufferedReader inputStream = new BufferedReader(new FileReader(inputFile));
             String wordLine = inputStream.readLine();
             while(wordLine!=null)
             {
              
                 String[] words = wordLine.trim().split("\\s+");
                 this.insert(words[0]);
                 int ind = search(words[0]);
                 TreeNode cn = (TreeNode)nodePool.elementAt(ind);
                 cn.freqCount = Integer.valueOf(words[1]).intValue();
                 nodePool.set(ind, cn);                 
                 wordLine = inputStream.readLine();
             }           
        }
        catch(Exception e)
        {
            System.out.println("Exception occurred in reading list file:" + e);
            e.printStackTrace();
            status = 0;
        }
        return status;
    }
    
    /***********************************************************************************   
    // Insert a  set of words into a tree (which bump the frequency if word already present )
    **********************************************************************************/
    public int  generateTreeFromWords(String[] words)
    {
        int status = 1;

        for (int i = 0; i < words.length; i++)
        {
             this.insert(words[i]);
             int ind = search(words[i]);
             TreeNode cn = (TreeNode)nodePool.elementAt(ind);
             nodePool.set(ind, cn);                 
         }           
        return status;
    }
  
      /***************************************************************************
     * convert the tree  into a string array that contain all the word in the tree(dictionary)
     ********************************************************************************** */
    // (Look for leaf nodes only)
    public String[] toStringList()
    {
        Vector rv = new Vector();
        TreeNode currentNode;
        int numOfNodes = nodePool.size();
        int currentInd = 1; //start from the second node, so ignore the null word 
        while(currentInd < numOfNodes)
        {
            currentNode = (TreeNode)nodePool.elementAt(currentInd);
            if (currentNode.splitChar==0) rv.add(getWord(currentInd));
            currentInd++;
        }
        String[] rs = new String[rv.size()];
        rv.toArray(rs);
        return rs;
    }
    
 /***************************************************************************
 * convert the tree  into a string array that contain all the word in the tree(dictionary)
 *  include the information of frequency to the list
 ********************************************************************************** */
    //
    public String[] toStringList2()
    {
        Vector rv = new Vector();
        TreeNode currentNode;
        int numOfNodes = nodePool.size();
        int currentInd = 1; //start from the second node, so ignore the null word 
        while(currentInd < numOfNodes)
        {
            currentNode = (TreeNode)nodePool.elementAt(currentInd);
            if (currentNode.splitChar==0) 
            {
                rv.add(getWord(currentInd)+ " " + getFrequency(currentInd));
            }
            currentInd++;
        }
        String[] rs = new String[rv.size()];
        rv.toArray(rs);
        return rs;
    }

    
 /*******************************************************************************
  * Write each word in the tree, along with its frequency to an output file
  * @param outfile
  **********************************************************************************/       
    protected int writeToFile2(String outfile)
    {
        String[] words = this.toStringList2();
        try
        {
            FileOutputStream fs1 = new FileOutputStream(outfile);
            for (int i = 0; i < words.length; i++)
            {
                fs1.write(words[i].getBytes());
                fs1.write('\n');
            }
            fs1.close();
            return 1;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("Error in  in writing to lexicon " + outfile);
            return 0;
        }
    }
}         

