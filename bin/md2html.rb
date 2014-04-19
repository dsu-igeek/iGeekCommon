#!/usr/bin/ruby

# Created by Kevin Wojniak on 3/14/11.
# Copyright 2011 Prosoft Engineering, Inc.

require 'cgi'
require 'pty'
require 'fileutils'

class Doc
   attr_accessor :metadata, :pages, :toc, :pageIDLinks
   def initialize
      @metadata = Hash.new
      @pages = Array.new
   end
end

class Page
   attr_accessor :id, :markdown, :title
   def initialize
      @markdown = Array.new
   end
end

class TOC
   attr_accessor :rootNode
   def initialize
      @rootNode = TOCNode.new
   end
end

class TOCNode
   attr_accessor :title, :pageID, :childNodes, :levelPrefix
   def initialize
      @childNodes = Array.new
   end
end

class ConditionalBlock
   attr_accessor :enabled, :name
end

# verify arguments
if ARGV.count < 4
   scriptPath = File.basename($0)
   puts "Usage: #{scriptPath} <file.mdown> <output directory> <TOC template.htm> <section template.htm>"
   exit 1
end

# verify file exists
file = ARGV[0]
if not File.exists?(file)
   puts "ERROR: File \"#{file}\" doesn't exist."
   exit 1
end

outputDir = ARGV[1]

$mmdPath = "../LocalizationTools/bin/MultiMarkdown/bin/MultiMarkdown.pl"
if not File.exists?($mmdPath)
   puts "ERROR: \"#{$mmdPath}\" doesn't exist."
   exit 1
end

$smartyPantsPath = "../LocalizationTools/bin/MultiMarkdown/bin/SmartyPants.pl"
if not File.exists?($smartyPantsPath)
   puts "ERROR: \"#{$smartyPantsPath}\" doesn't exist."
   exit 1
end

$tocTemplate = ARGV[2]
if not File.exists?($tocTemplate)
   puts "ERROR: \"#{$tocTemplate}\" doesn't exist."
   exit 1
end

$sectionTemplate = ARGV[3]
if not File.exists?($sectionTemplate)
   puts "ERROR: \"#{$sectionTemplate}\" doesn't exist."
   exit 1
end

# parse preprocessor definitions
$defines = Hash.new
argIndex = 4;
while argIndex < ARGV.size
   arg = ARGV[argIndex]
   defMatch = arg.match(/^-([A-Z0-9_]+)=(.*)$/)
   if not defMatch
      puts "ERROR: Invalid argument #{arg}"
      exit 1
   end
   key = defMatch[1]
   value = defMatch[2]
   $defines[key] = value
   argIndex = argIndex + 1
end

$doc = Doc.new
currentPage = nil
parsingMetadata = true

# run file through our (crude!) preprocessor before parsing any Markdown
def preprocessText(text)
   output = ""
   conditionals = Array.new

   text.split("\n").each do |line|
      currentConditional = conditionals.last

      # @if/@ifnot
      ifMatch = line.match(/^@(if[not]*)(.*)\s*$/)
      if ifMatch
         name = ifMatch[1]
         
         #if must contain an expression
         expression = ifMatch[2].strip
         if expression.length == 0
            puts "ERROR: @#{name} with no expression"
            exit 1
         end
         
         # expression must be specific syntax
         expressionMatch = expression.match(/^([A-Z0-9_]+)$/)
         enabled = false
         if expressionMatch
            definitionKey = expressionMatch[1]
            if $defines[definitionKey]
               enabled = true
            end
         else
            puts "ERROR: Invalid @#{name} expression."
            exit 1
         end

         if name == "ifnot"
            enabled = !enabled
         end
         
         # if the parent conditional is disabled, this is disabled too.
         if currentConditional and !currentConditional.enabled
            enabled = false
         end

         newConditional = ConditionalBlock.new
         newConditional.enabled = enabled
         newConditional.name = name
         conditionals.push(newConditional)
         next
      end
      
      # @else
      elseMatch = line.match(/^@(else)$/)
      if elseMatch
         if conditionals.length == 0
            puts "ERROR: @else without @if"
            exit 1
         end
         currentConditional.name = elseMatch[1]
         currentConditional.enabled = !currentConditional.enabled
         next
      end
      
      # @endif
      endIfMatch = line.match(/^@endif$/)
      if endIfMatch
         if conditionals.length == 0
            puts "ERROR: @endif without @if"
            exit 1
         end
         conditionals.pop
         next
      end
      
      if currentConditional and !currentConditional.enabled
         next
      end
      
      # replace definitions
      $defines.each_key do |key|
         while line[key]
            line[key] = $defines[key]
         end
      end
      
      output += "#{line}\n"
   end

   if conditionals.length > 0
      puts "ERROR: Unterminated @" + conditionals[-1].name
      exit 1
   end

   return output
end

text = File.read(file)
output = preprocessText(text)

# read preprocessed file
output.split("\n").each do |line|
   # parse metadata lines
   if parsingMetadata
      metaMatch = line.match(/^@@(.*?):\s*(.*?)\s*$/)
      if metaMatch
         $doc.metadata[metaMatch[1].downcase] = metaMatch[2]
      else
         parsingMetadata = false
      end
   end
   if parsingMetadata
      next
   end
   
   # parse new page title
   titleMatch = line.match(/^#\s+(.*?)\s*@@(.*?)\s*$/);
   if titleMatch
      currentPage = Page.new
      currentPage.title = titleMatch[1]
      currentPage.id = titleMatch[2]
      $doc.pages.push(currentPage)
   end
   
   if not currentPage
      next
   end
   
   currentPage.markdown.push(line)
end

# verify metadata
if not $doc.metadata["title"] or not $doc.metadata["toc-title"]
   puts "ERROR: Missing required title or toc-title metadata."
   exit 1
end

# verify 1 or more pages were parsed
if $doc.pages.size == 0
   puts "ERROR: No pages found."
   exit 1
end

# generate toc nodes from headers tagged with page IDs
$doc.toc = TOC.new
parentNodesStack = []
parentNodesStack.push($doc.toc.rootNode)
lastNode = nil
$doc.pages.each do |page|
   node = TOCNode.new
   node.title = page.title
   node.pageID = page.id
   
   while parentNodesStack.size > 1
      parentNodesStack.pop
   end
   parentNodesStack[0].childNodes.push(node)
   parentNodesStack.push(node)
   lastNode = node

   page.markdown.each do |line|
      sectionMatch = line.match(/^(##+)\s+(.*?)\s+@@(.*?)\s*$/);
      if not sectionMatch
         next
      end
      
      headerLevel = sectionMatch[1].length
      headerTitle = sectionMatch[2]
      pageID = sectionMatch[3]
      if headerLevel > parentNodesStack.size
         puts "ERROR: Invalid header level #{headerLevel} at \"#{headerTitle}\""
         exit 1
      end
      
      subNode = TOCNode.new
      subNode.title = headerTitle
      subNode.pageID = pageID
      
      if headerLevel == (parentNodesStack.size - 1)
         # same level node
         parentNodesStack[-2].childNodes.push(subNode)
         parentNodesStack[-1] = subNode
      elsif headerLevel == parentNodesStack.size
         # higher level node
         lastNode.childNodes.push(subNode)
         parentNodesStack.push(subNode)
      elsif headerLevel < parentNodesStack.size
         # lower level node
         i = (parentNodesStack.size - headerLevel - 2)
         while i >= 0
            parentNodesStack.pop
            i = i - 1
         end
         parentNodesStack[-2].childNodes.push(subNode)
         parentNodesStack[-1] = subNode
      end
      
      lastNode = subNode
   end
end

# output a TOC node and its children as HTML
def nodeAsHTML(node, level)
   output = ""
   indent = "\t" * level.size
   max = level.size
   
   special = false

   if node.title
      output += indent + '<dt>'
      
      prefix = ""
      index = 0
      maxIndex = level.size - 1
      level.each do |sect|
         prefix += sect.to_s
         if index < maxIndex
            prefix += "."
         end
         index = index + 1
      end
      node.levelPrefix = prefix
      
      output += "#{prefix}</dt>\n"

      output += indent + '<dd>'
      if node.pageID
         output += '<a href="' + linkToPageIDFrom(node.pageID, "toc") + '" target ="main">' + CGI::escapeHTML(node.title) + '</a>'
      else
         output += CGI::escapeHTML(node.title)
      end
      output += "</dd>\n"

      output += indent + '<dd class="special">' + "\n"
      special = true
   end
   
   if node.childNodes.size > 0
      level.push(1)
      output += indent + '<dl>' + "\n"
      node.childNodes.each do |childNode|
         output += nodeAsHTML(childNode, level)
      end
      output += indent + "</dl>\n"
      level.pop
   end
   
   if special
      output += indent + "</dd>\n"
   end
   
   if level.size > 0
      level[-1] = level[-1] + 1
   end
   return output
end

# convert MultiMarkdown to HTML
def markdownToHTML(markdown)
   output = ""
   IO.popen($mmdPath, "w+") do |f|
      f.print markdown
      f.close_write
      output = f.gets(nil)
   end
   return output
end

# get the TOC node for a page ID
def nodeWithID(pageID, node)
   if node.pageID == pageID
      return node
   end
   node.childNodes.each do |childNode|
      res = nodeWithID(pageID, childNode)
      if res
         return res
      end
   end
   return nil
end

# process our custom Markdown additions
def preprocessMarkdownForHTML(markdown)
   output = ""
   inInstructions = false
   
   markdown.split("\n").each do |line|
      # parse an instructions list
      # use a dummy HTML tag so our final regex doesn't get stuck in an infinite loop replacing itself
      instructionsMatch = line.match(/^>>\s*(.*?)$/)
      if instructionsMatch
         if not inInstructions
            output += "<instructions>\n"
         end
         output += instructionsMatch[1] + "\n"
         inInstructions = true
         next # don't try to parse anything else
      elsif inInstructions
         output += "</instructions>\n"
         inInstructions = false
      end

      # parse headers and page IDs
      headerMatch = line.match(/^(#+)\s+(.*?)\s+@@(.*?)$/)
      if headerMatch
         headerLevel = headerMatch[1].length.to_s
         headerTitle = headerMatch[2]
         headerID = headerMatch[3]
         node = nodeWithID(headerID, $doc.toc.rootNode)
         if not node
            puts "ERROR: Couldn't find node with ID #{headerID}"
            exit 1
         end
         output += "<h#{headerLevel}><a name=\"#{headerID}\">#{node.levelPrefix} #{headerTitle}</a></h#{headerLevel}>\n"
         next
      end
      
      # parse links to page IDs and replace with links to the real .htm file
      while 1
         linkMatch = line.match(/\[.*?\]\((@@(.*?))\)/)
         if linkMatch
            linkID = linkMatch[2]
            linkValue = linkToPageIDFrom(linkID, "_PAGE_") # use dummy value
            if not linkValue
               puts "ERROR: Invalid link ID \"#{linkID}\""
               exit 1
            end
            line[linkMatch[1]] = linkValue
         else
            break
         end
      end
      
      # parse image and label combo
      imgLabelMatch = line.match(/!!\[(.*?)\]\((.*?)\)/)
      if imgLabelMatch
         label = imgLabelMatch[1]
         imgPath = imgLabelMatch[2]
         
         # read the image and width height to force the size on images for better loading
         # when viewing the files in the boot DVD. there are some issues where anchor jump
         # links don't always jump to the right place on the boot DVD and apparently forcing
         # the image sizes allows the pages to jump properly.
   		imgWidth = 0
   		imgHeight = 1
   		begin
   		   data = nil
   		   if (imgPath =~ /.png$/)
   		      data = IO.read($pagesDir + "/" + imgPath, 8, 16).unpack('NN')
		      else
		         puts "ERROR: Unsupported image type: #{imgPath}"
		         exit 1
	         end
   		   if (data)
   		      imgWidth = data[0]
   		      imgHeight = data[1]
		      end
		   rescue
	      end
         imgWidthHeightAttrs = ""
         if imgWidth != 0 and imgHeight != 0
            imgWidthHeightAttrs = " width=\"#{imgWidth}\" height=\"#{imgHeight}\""
         end

         output += "<p class=\"imageAndLabel\"><img src=\"#{imgPath}\" alt=\"" + CGI::escapeHTML(label) + "\"#{imgWidthHeightAttrs}/><br/><em>" + CGI::escapeHTML(label) + "</em></p>\n"
         next
      end
      
      # parse warning paragraphs
      warningMatch = line.match(/^!!\s+(.*?)$/)
      if warningMatch
         output += "<warning>\n" + warningMatch[1] + "\n<\/warning>\n"
         next
      end
      
      output += line + "\n"
   end
   
   # close off an open instructions div
   if inInstructions
      output += "</instructions>\n"
   end
   
   # Markdown doesn't allow processing of markup within block-level tags such as <div>, so we have to manually process the markup.
   # We call preprocessMarkdownForHTML() to properly process our custom markup within these custom block elements.
   # An extra newline is added to force a paragraph
   while 1
      instructionsMatch = output.match(/(<instructions>)(.*?)(<\/instructions>)/m)
      if instructionsMatch
         output[instructionsMatch[1]] = "<div class=\"instructions\">"
         output[instructionsMatch[2]] = markdownToHTML(preprocessMarkdownForHTML("\n"+instructionsMatch[2]))
         output[instructionsMatch[3]] = "</div>"
      else
         break
      end
   end
   
   while 1
      warningMatch = output.match(/(<warning>)\s*(.*?)(<\/warning>)/m)
      if warningMatch
         output[warningMatch[1]] = "<div class=\"warning\"><div class=\"warningBody\"><div class=\"warningImg\"></div><div class=\"warningContent\">"
         output[warningMatch[2]] = markdownToHTML(preprocessMarkdownForHTML("\n"+warningMatch[2]))
         output[warningMatch[3]] = "</div></div></div>"
      else
         break
      end
   end
   
   return output
end

# run the text through SmartyPants
def textToSmartyPants(text)
   output = ""
   IO.popen($smartyPantsPath, "w+") do |f|
      f.print text
      f.close_write
      output = f.gets(nil)
   end
   return output
end

# map each page ID to its HTML link name
def generateLinks()
   links = {}
   
   $doc.pages.each do |page|
      page.markdown.each do |line|
         headerMatch = line.match(/^(#+)\s+(.*?)\s+@@(.*?)$/)
         if headerMatch
            headerLevel = headerMatch[1].length
            headerTitle = headerMatch[2]
            headerID = headerMatch[3]
            if links[headerID]
               puts "ERROR: #{headerID} already exists."
               exit 1
            end
            if headerLevel == 1
               links[headerID] = "#{page.id}.htm"
            else
               links[headerID] = "#{page.id}.htm##{headerID}"
            end
         end
      end
   end
   
   links["toc"] = "index.htm"
   
   return links
end

# generate a link to a page ID from another page ID
def linkToPageIDFrom(toPageID, fromPageID)
   puts "Link #{toPageID} to #{fromPageID}"
   link = $links[toPageID]
   if link == nil
	puts "Bad link for #{toPageID}"
   end
   if toPageID == fromPageID
      return link
   elsif fromPageID == "toc"
      link = "pages/" + link
   elsif toPageID == "toc" and fromPageID
      link = "../" + link
   end
   return link
end


if not File.exists?(outputDir) and not FileUtils.mkdir_p(outputDir)
   puts "ERROR: Couldn't create directory \"#{outputDir}\""
   exit 1
end

# create pages folder
$pagesDir = outputDir + "/pages"
if File.exists?($pagesDir) and not File.directory?($pagesDir)
   puts "ERROR: #{$pagesDir} exists but isn't a folder."
   exit 1
end
if not File.exists?($pagesDir)
   if not Dir.mkdir($pagesDir)
      puts "ERROR: Couldn't create directory #{$pagesDir}"
      exit 1
   end
end

$links = generateLinks()

# write toc
tocHTML = File.read($tocTemplate)
if not tocHTML
   puts "ERROR: Couldn't read TOC template."
   exit 1
end
while tocHTML["$DOC_TITLE$"]
   tocHTML["$DOC_TITLE$"] = CGI::escapeHTML($doc.metadata["title"])
end
while tocHTML["$TITLE$"]
   tocHTML["$TITLE$"] = CGI::escapeHTML($doc.metadata["toc-title"])
end
while tocHTML["$TOC_TITLE$"]
   tocHTML["$TOC_TITLE$"] = CGI::escapeHTML($doc.metadata["toc-title"])
end
if tocHTML["$BODY$"]
   tocHTML["$BODY$"] = nodeAsHTML($doc.toc.rootNode, Array.new).strip
end
while tocHTML["$FOLDER_NAME$"]
   tocHTML["$FOLDER_NAME$"] = CGI::escapeHTML(File.basename(outputDir))
end
path = outputDir + "/" + linkToPageIDFrom("toc", nil)
file = File.new(path, "w")
if not file
   puts "ERROR: Couldn't open file for writing \"#{path}\""
   exit 1
end
if not file.write(tocHTML)
   puts "ERROR: Couldn't write file \"#{path}\""
   exit 1
end
file.close

# write sections
index = 0
max = $doc.pages.size
while index < max
   page = $doc.pages[index]

   sectHTML = File.read($sectionTemplate)
   if not sectHTML
      puts "ERROR: Couldn't read section template."
      exit 1
   end
   
   while sectHTML["$TITLE$"]
      sectHTML["$TITLE$"] = CGI::escapeHTML(page.title)
   end
   markdownText = preprocessMarkdownForHTML(page.markdown.join("\n"))
   mmdHTML = textToSmartyPants(markdownToHTML(markdownText)).strip

   while sectHTML["$DOC_TITLE$"]
      sectHTML["$DOC_TITLE$"] = CGI::escapeHTML($doc.metadata["title"])
   end
   sectHTML["$BODY$"] = mmdHTML
   while sectHTML["$TOC_TITLE$"]
      sectHTML["$TOC_TITLE$"] = CGI::escapeHTML($doc.metadata["toc-title"])
   end
   while sectHTML["$TOC_LINK$"]
      sectHTML["$TOC_LINK$"] = linkToPageIDFrom("toc", page.id)
   end
   
   prevPage = nil
   if index > 0
      prevPage = $doc.pages[index - 1]
   end
   if prevPage
      while sectHTML["$PREV_TITLE$"]
         sectHTML["$PREV_TITLE$"] = CGI::escapeHTML(prevPage.title)
      end
      while sectHTML["$PREV_LINK$"]
         sectHTML["$PREV_LINK$"] = linkToPageIDFrom(prevPage.id, page.id)
      end
      while sectHTML["$DISPLAY_PREV$"]
         sectHTML["$DISPLAY_PREV$"] = "visible";
      end
   else
      while sectHTML["$DISPLAY_PREV$"]
         sectHTML["$DISPLAY_PREV$"] = "none";
      end
      # dummy data, this is not shown unless CSS is off
      while sectHTML["$PREV_TITLE$"]
         sectHTML["$PREV_TITLE$"] = $doc.metadata["toc-title"]
      end
      while sectHTML["$PREV_LINK$"]
         sectHTML["$PREV_LINK$"] = linkToPageIDFrom("toc", page.id)
      end
   end
   
   nextPage = nil
   if index < (max - 1)
      nextPage = $doc.pages[index + 1]
   end
   if nextPage
      while sectHTML["$NEXT_TITLE$"]
         sectHTML["$NEXT_TITLE$"] = CGI::escapeHTML(nextPage.title)
      end
      while sectHTML["$NEXT_LINK$"]
         sectHTML["$NEXT_LINK$"] = linkToPageIDFrom(nextPage.id, page.id)
      end
      while sectHTML["$DISPLAY_NEXT$"]
         sectHTML["$DISPLAY_NEXT$"] = "visible";
      end
   else
      while sectHTML["$DISPLAY_NEXT$"]
         sectHTML["$DISPLAY_NEXT$"] = "none";
      end
      # dummy data, this is not shown unless CSS is off
      while sectHTML["$NEXT_TITLE$"]
         sectHTML["$NEXT_TITLE$"] = $doc.metadata["toc-title"]
      end
      while sectHTML["$NEXT_LINK$"]
         sectHTML["$NEXT_LINK$"] = linkToPageIDFrom("toc", page.id)
      end
   end
   
   path = $pagesDir + "/" + linkToPageIDFrom(page.id, nil)
   file = File.new(path, "w")
   if not file
      puts "ERROR: Couldn't open file for writing \"#{path}\""
      exit 1
   end
   if not file.write(sectHTML)
      puts "ERROR: Couldn't write file \"#{path}\""
      exit 1
   end
   file.close

   index += 1
end
