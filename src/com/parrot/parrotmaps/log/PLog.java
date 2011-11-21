/*
 * Copyright (c) 2011, Parrot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   * Neither the name of "Parrot SA" nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.parrot.parrotmaps.log;

/**
 * Wrapper for Log.v, Log.d, Log.i, Log.w and Log.e methods.
 * 
 * @author FL
 *
 */
public class PLog {
    public static int LEVEL = android.util.Log.ERROR;

    static public void v(String tag, String msg, Object...args)
    {
        if (LEVEL<=android.util.Log.VERBOSE)
        {
        	if( args.length < 1 ) {
                android.util.Log.v(tag, msg);
        	}
        	else {
            	StringBuilder builder = new StringBuilder(msg);
            	for( int i=0; i<args.length; i++ )
            		builder.append(args[i]);
                android.util.Log.v(tag, builder.toString());
        	}
        }
    }

    static public void d(String tag, String msg, Object...args)
    {
        if (LEVEL<=android.util.Log.DEBUG)
        {
        	if( args.length < 1 ) {
                android.util.Log.d(tag, msg);
        	}
        	else {
            	StringBuilder builder = new StringBuilder(msg);
            	for( int i=0; i<args.length; i++ )
            		builder.append(args[i]);
                android.util.Log.d(tag, builder.toString());
        	}
        }
    }

    static public void i(String tag, String msg, Object...args)
    {
        if (LEVEL<=android.util.Log.INFO)
        {
        	if( args.length < 1 ) {
                android.util.Log.i(tag, msg);
        	}
        	else {
            	StringBuilder builder = new StringBuilder(msg);
            	for( int i=0; i<args.length; i++ )
            		builder.append(args[i]);
                android.util.Log.i(tag, builder.toString());
        	}
        }
    }

    static public void w(String tag, String msg, Object...args)
    {
        if (LEVEL<=android.util.Log.WARN)
        {
        	if( args.length < 1 ) {
                android.util.Log.w(tag, msg);
        	}
        	else {
            	StringBuilder builder = new StringBuilder(msg);
            	for( int i=0; i<args.length; i++ )
            		builder.append(args[i]);
                android.util.Log.w(tag, builder.toString());
        	}
        }
    }

    static public void e(String tag, String msg, Object...args)
    {
        if (LEVEL<=android.util.Log.ERROR)
        {
        	if( args.length < 1 ) {
                android.util.Log.e(tag, msg);
        	}
        	else {
            	StringBuilder builder = new StringBuilder(msg);
            	for( int i=0; i<args.length; i++ )
            		builder.append(args[i]);
                android.util.Log.e(tag, builder.toString());
        	}
        }
    }
 }
