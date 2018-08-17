require 'parser/ruby25'
require 'java'

# Custom builder in order to avoid throwing an error when literals contain an escape sequences incompatible with UTF-8
# See https://github.com/whitequark/parser/issues/283
# Inspired from https://github.com/eapache/starscope/pull/166
class Builder < Parser::Builders::Default

  # opt-in to most recent AST format (used for Backwards compatibility when breaking changes are introduced in AST format)
  # In order not to break backward compatibility, when breaking changes are introduced in a newer version of the parser AST, these new
  # features have to be manually enabled. Here we enable the latest/current features at time of development.
  @emit_lambda = true
  @emit_procarg0 = true
  @emit_encoding = true
  @emit_index = true

  def string_value(token)
    value(token)
  end
end

# In order to retrieve AST, comments, and tokens, we need to use the 'tokenize' method of the ruby Parser object.
# However, the 'tokenize' method takes directly a Buffer object as parameter. Here, we map the string content to the Buffer object the
# same way it is done in the 'Parser::Base.parse' and 'Parser::Base.setup_source_buffer' methods.
def parse_with_tokens(content, filename)
  parser = Parser::Ruby25.new(Builder.new)
  parser.diagnostics.all_errors_are_fatal = true
  parser.diagnostics.ignore_warnings = true

  content = content.dup.force_encoding(parser.default_encoding)
  source_buffer = Parser::Source::Buffer.new(filename, 1)
  source_buffer.source = content
  parser.tokenize(source_buffer)
end

def visit(ast, visitor)
  processor = ProcessorBridge.new(visitor)
  processor.process(ast)
end

# Bridge between RubyVisitor and Parser::AST::Processor APIs
class ProcessorBridge < Parser::AST::Processor

  def initialize(visitor)
    @visitor = visitor
  end

  def process(node)
    return if node.nil?
    @visitor.beforeVisit(AstNode.new(node))
    node = super
    tree = @visitor.visitNode(AstNode.new(node), node.to_a)
    @visitor.afterVisit(tree)
    tree
  end

end

# Bridge between Parser::AST::Node and AstNode
class AstNode
  include org.sonarsource.ruby.converter.AstNode

  def initialize(node)
    @node = node
  end

  def type
    if @node.respond_to?(:type)
      @node.type
    else
      @node.to_s
    end
  end

  def asString
    @node.to_s
  end

  def textRange()
    textRangeForAttribute('expression')
  end

  def textRangeForAttribute(attr)
    if @node.location.respond_to?(attr)
      loc = @node.location.public_send(attr)
      return if loc.nil?
      textRangeFromLocation(loc)
    end
  end

  def textRangeFromLocation(loc)
    org.sonarsource.slang.impl.TextRangeImpl.new(loc.line, loc.column, loc.last_line, loc.last_column)
  end

  def node
    @node
  end

end
