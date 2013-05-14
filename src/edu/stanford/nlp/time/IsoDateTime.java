package edu.stanford.nlp.time;



class IsoDateTime extends PartialTime {
    IsoDate date;
    IsoTime time;

    public IsoDateTime(IsoDate date, IsoTime time) {
        this.date = date;
        this.time = time;
        base = JodaTimeUtils.combine(date.base, time.base);
    }

    public boolean hasTime() {
        return time != null;
    }
    /*    public String toISOString()
    {
    return date.toISOString() + time.toISOString();
    }  */
    private static final long serialVersionUID = 1;

}
