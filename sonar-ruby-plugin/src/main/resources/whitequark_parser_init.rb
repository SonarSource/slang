require "java"
require 'parser/ruby25'

java_import "org.sonarsource.slang.impl.TextRanges"
java_import "org.sonarsource.slang.impl.NativeTreeImpl"
java_import "org.sonarsource.ruby.converter.RubyNativeKind"

java_import "org.sonarsource.slang.impl.ClassDeclarationTreeImpl"

# opt-in to most recent AST format (used for Backwards compatibility when breaking changes are introduced in AST format)
# In order not to break backward compatibility, when breaking changes are introduced in a newer version of the parser AST, these new
# features have to be manually enabled. Here we enable the latest/current features at time of development.
Parser::Builders::Default.emit_lambda   = true
Parser::Builders::Default.emit_procarg0 = true
Parser::Builders::Default.emit_encoding = true
Parser::Builders::Default.emit_index    = true

# In order to retrieve AST, comments, and tokens, we need to use the 'tokenize' method of the ruby Parser object.
# However, the 'tokenize' method takes directly a Buffer object as parameter. Here, we map the string content to the Buffer object the
# same way it is done in the 'Parser::Base.parse' and 'Parser::Base.setup_source_buffer' methods.
def parse_with_tokens(content, filename='(string)')
  parser = Parser::Ruby25.default_parser
  content = content.dup.force_encoding(parser.default_encoding)
  source_buffer = Parser::Source::Buffer.new(filename, 1)
  source_buffer.source = content
  parser.tokenize(source_buffer)
end


def convert_to_slang(rubyTree, metaDataProvider)
  RubyProcessor.new(metaDataProvider).process(rubyTree)
end

def getTextRange(node)
  loc = node.location;
  # puts "#{loc.line} #{loc.column} #{loc.last_line} #{loc.last_column}"
  TextRanges.range(loc.line, loc.column, loc.last_line, loc.last_column)
end


def toNative(node, metadataProvider)
  slangChildren = node.to_a.select { |child| !child.nil? && child.kind_of?(Parser::AST::Node) }.map{ |child| child.slang}

  metadata = @metaDataProvider.metaData(getTextRange(node))
  puts slangChildren
  NativeTreeImpl.new(metadata, RubyNativeKind.new(node.type), slangChildren)
end

class RubyProcessor < Parser::AST::Processor
  def initialize(metaDataProvider)
    @metaDataProvider = metaDataProvider
  end

  def process(node)
    if node.nil?
      return
    end

    puts "#{node.type}"
    puts "#{node}"
    node = super node
# if no slang on node
    addSlangNode(node, toNative(node, @metaDataProvider))

    node
  end

  def on_class(node)
    super node
    puts "class!!!"
    metadata = @metaDataProvider.metaData(getTextRange(node))

 #fixme identifier
    addSlangNode(node, ClassDeclarationTreeImpl.new(metadata, nil, toNative(node, @metaDataProvider)));
    puts node.slang
    node
  end
end


def addSlangNode(node, slangNode)
  node.singleton_class.module_eval { attr_accessor :slang }
  node.slang = slangNode
end

class NodeAdapter < Parser::AST::Node

end
