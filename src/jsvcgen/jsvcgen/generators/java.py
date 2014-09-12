"""Copyright (c) 2014 by Travis Gockel. All rights reserved.

This program is free software: you can redistribute it and/or modify it under the terms of the Apache License
as published by the Apache Software Foundation, either version 2 of the License, or (at your option) any later
version.

Travis Gockel (travis@gockelhut.com)"""
from . import detail

TYPENAME_MAPPING = {
    'boolean': 'boolean',
    'integer': 'long',
    'number': 'double',
    'string': 'String',
}

class JavaGenerator(detail.BaseGenerator):
    def __init__(self, namespace, immutable_types=False, **kwargs):
        super().__init__(**kwargs)
        self._namespace = namespace
        self._immutable_types = immutable_types
    
    @classmethod
    def create_with_args(cls, args):
        return JavaGenerator(namespace=args.namespace)
    
    def get_filename_for(self, output_path, entity, svcdesc):
        return output_path + '/' + self._namespace.replace('.', '/') + '/' + self.format_typename(entity) + '.java'
    
    def get_arg_list(self, args):
        return ', '.join((self.format_typename(x.type) + ' ' + self.format_fieldname(x.name)) for x in args)
    
    def get_includes(self, entity, svcdesc):
        yield 'import java.lang;'
    
    def get_fields(self, typ, svcdesc):
        for member in typ.members:
            yield self.indent(1) + 'private ' + ('final ' if self._immutable_types else '') + self.format_typename(member.type) + ' ' + self.format_fieldname(member.name) + ';'
    
    def get_file_header(self, entity, svcdesc):
        yield 'package ' + self._namespace + ';'
        yield ''
    
    def get_member_accessor(self, member, svcdesc):
        if member.documentation:
            yield detail.comment_c_style(member.documentation, self.indent(1))
        yield self.indent(1) + 'public ' + self.format_typename(member.type) + ' ' + self.format_accessor(member.name) + '() {'
        yield self.indent(2) + 'return this.' + self.format_fieldname(member.name) + ';'
        yield self.indent(1) + '}'
    
    def get_member_mutator(self, member, svcdesc):
        yield self.indent(1) + 'public void ' + self.format_mutator(member.name) + '(' + self.format_typename(member.type) + ' ' + self.format_fieldname(member.name) + ') {'
        yield self.indent(2) + 'this.' + self.format_fieldname(member.name) + ' = ' + self.format_fieldname(member.name) + ';'
        yield self.indent(1) + '}'
    
    def get_type_ctor(self, typ, svcdesc):
        ctor_arg_list = self.get_arg_list(typ.members)
        
        yield self.indent(1) + 'public ' + self.format_typename(typ) + '(' + ctor_arg_list + ') {'
        for member in typ.members:
            yield self.indent(2) + 'this.' + self.format_fieldname(member.name) + ' = ' + self.format_fieldname(member.name) + ';'
        yield self.indent(1) + '}'
    
    def get_type(self, typ, svcdesc):
        if typ.documentation:
            yield detail.comment_c_style(typ.documentation)
        
        yield 'public class ' + self.format_typename(typ) + ' {'
        
        for line in self.get_fields(typ, svcdesc):
            yield line
        yield ''
        
        for line in self.get_type_ctor(typ, svcdesc):
            yield line
        yield ''
        
        for member in typ.members:
            for line in self.get_member_accessor(member, svcdesc):
                yield line
            yield ''
            if not self._immutable_types:
                for line in self.get_member_mutator(member, svcdesc):
                    yield line
                yield ''
        
        yield '}'
    
    def get_service(self, svcdesc):
        yield 'public class ' + self.format_typename(svcdesc) + ' extends com.gockelhut.jsvcgen.JsonRpcServiceBase {'
        yield self.indent(1) + 'public ' + self.format_typename(svcdesc) + '(URL endpoint) {'
        yield self.indent(2) + 'super(endpoint);'
        yield self.indent(1) + '}'
        
        for method in svcdesc.methods:
            for line in self.get_method(method, svcdesc):
                yield line
            yield ''
        
        yield '}'
    
    def get_method(self, method, svcdesc):
        yield detail.comment_c_style(method.documentation, self.indent(1))
        yield self.indent(1) + 'public ' + self.format_typename(method.return_info.type) + ' ' + self.format_method_name(method) \
            + '(' + self.get_arg_list(method.params) + ') {'
        yield self.indent(2) + '// TODO'
        yield self.indent(1) + '}'
    
    def format_accessor(self, membername):
        return 'get' + detail.camel_case(membername, True)
    
    def format_fieldname(self, fieldname):
        return detail.camel_case(fieldname, False)
    
    def format_method_name(self, method):
        return detail.camel_case(method.name, False)
    
    def format_mutator(self, membername):
        return 'set' + detail.camel_case(membername, True)
    
    def format_typename(self, typ):
        typename = TYPENAME_MAPPING.get(typ.name, typ.name)
        if typ.is_array:
            typename += '[]'
        return typename
