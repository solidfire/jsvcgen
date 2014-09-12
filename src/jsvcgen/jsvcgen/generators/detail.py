"""Copyright (c) 2014 by Travis Gockel. All rights reserved.

This program is free software: you can redistribute it and/or modify it under the terms of the Apache License
as published by the Apache Software Foundation, either version 2 of the License, or (at your option) any later
version.

Travis Gockel (travis@gockelhut.com)"""
import os

class BaseGenerator(object):
    def __init__(self, newline='\n', indent='    '):
        self._newline = newline
        self._indent = indent
    
    def indent(self, level):
        return self._indent * level
    
    def get_file_header(self, entity, svcdesc):
        return tuple()
    
    def get_includes(self, entity, svcdesc):
        return tuple()
    
    def generate(self, wsp, output_path):
        for typ in wsp.types:
            with self.open_file_for(output_path, typ, wsp) as output:
                for line in self.get_type(typ, wsp):
                    output.write(str(line))
                    output.write(self._newline)
        
        with self.open_file_for(output_path, wsp, wsp) as output:
            for line in self.get_service(wsp):
                output.write(line)
                output.write(self._newline)
    
    def open_file_for(self, output_path, entity, svcdesc):
        path = self.get_filename_for(output_path, entity, svcdesc)
        if not os.path.exists(os.path.dirname(path)):
            os.makedirs(os.path.dirname(path))
        f = open(path, 'w+')
        try:
            self.write_header(f, entity, svcdesc)
            return f
        except:
            f.close()
            raise
    
    def format_fieldname(self, fieldname):
        return fieldname
    
    def format_typename(self, typ):
        return typ.name + ('[]' if typ.is_array else '')
    
    def write_header(self, f, entity, svcdesc):
        for line in self.get_file_header(entity, svcdesc):
            f.write(line)
            f.write(self._newline)
        
        for line in self.get_includes(entity, svcdesc):
            f.write(line)
            f.write(self._newline)

def comment_c_style(src, line_prefix='', max_width=80, newline='\n'):
    ss = line_prefix + '/**' + newline
    
    while src:
        current_line = line_prefix + ' * '
        next_newline = src.find('\n')
        next_newline = len(src) if next_newline == -1 else next_newline
        cutpoint = min(next_newline, max_width - len(current_line))
        wsidx = src[:cutpoint].rfind(' ')
        if wsidx == -1:
            wsidx = cutpoint
        current_line += src[:wsidx]
        ss += current_line + newline
        src = src[wsidx+1:]
    
    return ss + line_prefix + '**/'

def camel_case(src, first_upper=False):
    out = ''
    next_upper = first_upper
    for c in src:
        if c == '_':
            next_upper = True
        elif next_upper:
            out += c.upper()
            next_upper = False
        else:
            out += c
    return out

assert camel_case('get_lines', False) == 'getLines'
assert camel_case('get_lines', True)  == 'GetLines'
