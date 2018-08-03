require 'parser/ruby25'

# opt-in to most recent AST format (used for Backwards compatibility when breaking changes are introduced in AST format)
Parser::Builders::Default.emit_lambda   = true
Parser::Builders::Default.emit_procarg0 = true
Parser::Builders::Default.emit_encoding = true
Parser::Builders::Default.emit_index    = true

# Parser initialization for 'tokenize' method based on 'Parser::Base.parse' and 'Parser::Base.setup_source_buffer'
def parse_with_tokens(content, filename='(string)')
  parser = Parser::Ruby25.default_parser
  content = content.dup.force_encoding(parser.default_encoding)
  source_buffer = Parser::Source::Buffer.new(filename, 1)
  source_buffer.source = content
  parser.tokenize(source_buffer)
end
