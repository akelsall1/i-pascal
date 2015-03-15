unit Simple;

interface

uses
    SysUtils, StrUtils,
    //aaa
    types;

type
    TRecord = record
        Name2, CurrentValue, DefaultValue: AnsiString;
        HashValue: LongWord;
        {class function a: int;}
        constructor b(a, b: int);
    end;

    TEnum = (eOne,
        // dasd
        eTwo,
        //sda
        eThree);

    TObj = object
        Name2, CurrentValue, DefaultValue: AnsiString;
        HashValue: LongWord;
        procedure ba(); virtual; abstract;
        //class function a: int;
        constructor b(a, b: int);
        property cc: Int read ba; default;
    end;

    TVector2s = packed record
    case Integer of
        0:(X, Y: Single);
        1:(V: array[0..1] of Single);
    end;

    TC = class(TVector2s)
    private
        name: string;
    end;

    TStringSearchOption = (soDown, soMatchCase, soWholeWord);
    TStringSearchOptions = set of TStringSearchOption;TStringSeachOption = TStringSearchOption;


    Function LeftStr(const AText: AnsiString; const ACount: Integer): AnsiString; inline;
    Function RightStr(const AText: AnsiString; const ACount: Integer): AnsiString; register;
    Function MidStr(const AText: AnsiString; const AStart, ACount: Integer): AnsiString; inline;

implementation

Function LeftStr(const AText: AnsiString; const ACount: Integer): AnsiString; register; overload;
var
    Value: TC;
begin
    Value := 2 <> (aa=1);
    Value.X;
    if ((Value mod 2)=1) then
        raise Exception.Create('MyProp can only contain even value');
    FMyInt := Value;
end;

Function RightStr (const AText: AnsiString; const ACount: Integer): AnsiString;
const
    cc: TCHelper = ();
var
    j: Integer;
    i: TRecord;
begin
    j := length(ASubText);
    i.CurrentValue;
    cc.DefaultValue;
    if ACount>i then
        aStart := i+1;
    k := i+1-AStart;
    if ALength> k then
        ALength := k;
    SetLength(Result, i+j-ALength);
    move(AText[1], result[1], AStart-1);
    move(ASubText[1], result[AStart], j);
    move(AText[AStart+ALength], Result[AStart+j], i+1-AStart-ALength);
end;

Function MidStr(const AText: AnsiString; const AStart, ACount: Integer): AnsiString;
begin
    with ResourceStringTable do begin
        for i:=0 to Count-1 do begin
            ResStr := Tables[I].TableStart;
            // Skip first entry (name of the Unit) }
            inc(ResStr);
            while ResStr<Tables[I].TableEnd do
            begin
                ResStr^.CurrentValue := '';
                inc(ResStr);
            end;
        end;
    end;
end;

begin
end.
