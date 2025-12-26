/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package compression.arithmaticCoding.bigDecimalAc;
import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.util.List;

/**
 * Interface for our general abstraction of arithmetic decoding.
 */
public interface ArithmeticDecoder {

    Interval decodeNext(List<Interval> options );
    
}
