module xillydemo
  (
  input  PS_CLK,
  input  PS_PORB,
  input  PS_SRSTB,
  input  clk_100,
  input  otg_oc,   
  inout [14:0] DDR_Addr,
  inout [2:0] DDR_BankAddr,
  inout  DDR_CAS_n,
  inout  DDR_CKE,
  inout  DDR_CS_n,
  inout  DDR_Clk,
  inout  DDR_Clk_n,
  inout [3:0] DDR_DM,
  inout [31:0] DDR_DQ,
  inout [3:0] DDR_DQS,
  inout [3:0] DDR_DQS_n,
  inout  DDR_DRSTB,
  inout  DDR_ODT,
  inout  DDR_RAS_n,
  inout  DDR_VRN,
  inout  DDR_VRP,
  inout [53:0] MIO,
  inout [55:0] PS_GPIO,
  output  DDR_WEB,
  output [3:0] GPIO_LED,
  output [3:0] vga4_blue,
  output [3:0] vga4_green,
  output [3:0] vga4_red,
  output  vga_hsync,
  output  vga_vsync,

  output  audio_mclk,
  output  audio_dac,
  input   audio_adc,
  input   audio_bclk,
  input   audio_lrclk,

  output smb_sclk,
  inout  smb_sdata,
  output [1:0] smbus_addr

  ); 
   // Clock and quiesce
   wire    bus_clk;
   wire    quiesce;

   // Memory arrays
   reg [7:0] demoarray[0:31];
   
   reg [7:0] litearray0[0:31];
   reg [7:0] litearray1[0:31];
   reg [7:0] litearray2[0:31];
   reg [7:0] litearray3[0:31];

   // Wires related to /dev/xillybus_mem_8
   wire      user_r_mem_8_rden;
   wire      user_r_mem_8_empty;
   reg [7:0] user_r_mem_8_data;
   wire      user_r_mem_8_eof;
   wire      user_r_mem_8_open;
   wire      user_w_mem_8_wren;
   wire      user_w_mem_8_full;
   wire [7:0] user_w_mem_8_data;
   wire       user_w_mem_8_open;
   wire [4:0] user_mem_8_addr;
   wire       user_mem_8_addr_update;

   // Wires related to /dev/xillybus_read_32
   wire       user_r_read_32_rden;
   wire       user_r_read_32_empty;
   wire [31:0] user_r_read_32_data;
   wire        user_r_read_32_eof;
   wire        user_r_read_32_open;

   // Wires related to /dev/xillybus_read_8
   wire        user_r_read_8_rden;
   wire        user_r_read_8_empty;
   wire [7:0]  user_r_read_8_data;
   wire        user_r_read_8_eof;
   wire        user_r_read_8_open;

   // Wires related to /dev/xillybus_write_32
   wire        user_w_write_32_wren;
   wire        user_w_write_32_full;
   wire [31:0] user_w_write_32_data;
   wire        user_w_write_32_open;

   // Wires related to /dev/xillybus_write_8
   wire        user_w_write_8_wren;
   wire        user_w_write_8_full;
   wire [7:0]  user_w_write_8_data;
   wire        user_w_write_8_open;

   // Wires related to /dev/xillybus_audio
   wire        user_r_audio_rden;
   wire        user_r_audio_empty;
   wire [31:0] user_r_audio_data;
   wire        user_r_audio_eof;
   wire        user_r_audio_open;
   wire        user_w_audio_wren;
   wire        user_w_audio_full;
   wire [31:0] user_w_audio_data;
   wire        user_w_audio_open;
 
   // Wires related to /dev/xillybus_smb
   wire        user_r_smb_rden;
   wire        user_r_smb_empty;
   wire [7:0]  user_r_smb_data;
   wire        user_r_smb_eof;
   wire        user_r_smb_open;
   wire        user_w_smb_wren;
   wire        user_w_smb_full;
   wire [7:0]  user_w_smb_data;
   wire        user_w_smb_open;

   // Wires related to Xillybus Lite
   wire        user_clk;
   wire        user_wren;
   wire [3:0]  user_wstrb;
   wire        user_rden;
   reg [31:0]  user_rd_data;
   wire [31:0] user_wr_data;
   wire [31:0] user_addr;
   wire        user_irq;
   
   xillybus xillybus_ins (

    // Ports related to /dev/xillybus_mem_8
    // FPGA to CPU signals:
    .user_r_mem_8_rden(user_r_mem_8_rden),
    .user_r_mem_8_empty(user_r_mem_8_empty),
    .user_r_mem_8_data(user_r_mem_8_data),
    .user_r_mem_8_eof(user_r_mem_8_eof),
    .user_r_mem_8_open(user_r_mem_8_open),

    // CPU to FPGA signals:
    .user_w_mem_8_wren(user_w_mem_8_wren),
    .user_w_mem_8_full(user_w_mem_8_full),
    .user_w_mem_8_data(user_w_mem_8_data),
    .user_w_mem_8_open(user_w_mem_8_open),

    // Address signals:
    .user_mem_8_addr(user_mem_8_addr),
    .user_mem_8_addr_update(user_mem_8_addr_update),


    // Ports related to /dev/xillybus_read_32
    // FPGA to CPU signals:
    .user_r_read_32_rden(user_r_read_32_rden),
    .user_r_read_32_empty(user_r_read_32_empty),
    .user_r_read_32_data(user_r_read_32_data),
    .user_r_read_32_eof(user_r_read_32_eof),
    .user_r_read_32_open(user_r_read_32_open),


    // Ports related to /dev/xillybus_read_8
    // FPGA to CPU signals:
    .user_r_read_8_rden(user_r_read_8_rden),
    .user_r_read_8_empty(user_r_read_8_empty),
    .user_r_read_8_data(user_r_read_8_data),
    .user_r_read_8_eof(user_r_read_8_eof),
    .user_r_read_8_open(user_r_read_8_open),


    // Ports related to /dev/xillybus_write_32
    // CPU to FPGA signals:
    .user_w_write_32_wren(user_w_write_32_wren),
    .user_w_write_32_full(user_w_write_32_full),
    .user_w_write_32_data(user_w_write_32_data),
    .user_w_write_32_open(user_w_write_32_open),


    // Ports related to /dev/xillybus_write_8
    // CPU to FPGA signals:
    .user_w_write_8_wren(user_w_write_8_wren),
    .user_w_write_8_full(user_w_write_8_full),
    .user_w_write_8_data(user_w_write_8_data),
    .user_w_write_8_open(user_w_write_8_open),

    // Ports related to /dev/xillybus_audio
    // FPGA to CPU signals:
    .user_r_audio_rden(user_r_audio_rden),
    .user_r_audio_empty(user_r_audio_empty),
    .user_r_audio_data(user_r_audio_data),
    .user_r_audio_eof(user_r_audio_eof),
    .user_r_audio_open(user_r_audio_open),

    // CPU to FPGA signals:
    .user_w_audio_wren(user_w_audio_wren),
    .user_w_audio_full(user_w_audio_full),
    .user_w_audio_data(user_w_audio_data),
    .user_w_audio_open(user_w_audio_open),

    // Ports related to /dev/xillybus_smb
    // FPGA to CPU signals:
    .user_r_smb_rden(user_r_smb_rden),
    .user_r_smb_empty(user_r_smb_empty),
    .user_r_smb_data(user_r_smb_data),
    .user_r_smb_eof(user_r_smb_eof),
    .user_r_smb_open(user_r_smb_open),

    // CPU to FPGA signals:
    .user_w_smb_wren(user_w_smb_wren),
    .user_w_smb_full(user_w_smb_full),
    .user_w_smb_data(user_w_smb_data),
    .user_w_smb_open(user_w_smb_open),

    // Xillybus Lite signals:
    .user_clk ( user_clk ),
    .user_wren ( user_wren ),
    .user_wstrb ( user_wstrb ),
    .user_rden ( user_rden ),
    .user_rd_data ( user_rd_data ),
    .user_wr_data ( user_wr_data ),
    .user_addr ( user_addr ),
    .user_irq ( user_irq ),
			  			  
    // General signals
    .PS_CLK(PS_CLK),
    .PS_PORB(PS_PORB),
    .PS_SRSTB(PS_SRSTB),
    .clk_100(clk_100),
    .otg_oc(otg_oc),
    .DDR_Addr(DDR_Addr),
    .DDR_BankAddr(DDR_BankAddr),
    .DDR_CAS_n(DDR_CAS_n),
    .DDR_CKE(DDR_CKE),
    .DDR_CS_n(DDR_CS_n),
    .DDR_Clk(DDR_Clk),
    .DDR_Clk_n(DDR_Clk_n),
    .DDR_DM(DDR_DM),
    .DDR_DQ(DDR_DQ),
    .DDR_DQS(DDR_DQS),
    .DDR_DQS_n(DDR_DQS_n),
    .DDR_DRSTB(DDR_DRSTB),
    .DDR_ODT(DDR_ODT),
    .DDR_RAS_n(DDR_RAS_n),
    .DDR_VRN(DDR_VRN),
    .DDR_VRP(DDR_VRP),
    .MIO(MIO),
    .PS_GPIO(PS_GPIO),
    .DDR_WEB(DDR_WEB),
    .GPIO_LED(GPIO_LED),
    .bus_clk(bus_clk),
    .quiesce(quiesce),

    // VGA port related outputs
			    
    .vga4_blue(vga4_blue),
    .vga4_green(vga4_green),
    .vga4_red(vga4_red),
    .vga_hsync(vga_hsync),
    .vga_vsync(vga_vsync)
  );

   assign      user_irq = 0; // No interrupts for now
   
   always @(posedge user_clk)
     begin
	if (user_wstrb[0])
	  litearray0[user_addr[6:2]] <= user_wr_data[7:0];

	if (user_wstrb[1])
	  litearray1[user_addr[6:2]] <= user_wr_data[15:8];

	if (user_wstrb[2])
	  litearray2[user_addr[6:2]] <= user_wr_data[23:16];

	if (user_wstrb[3])
	  litearray3[user_addr[6:2]] <= user_wr_data[31:24];
	
	if (user_rden)
	  user_rd_data <= { litearray3[user_addr[6:2]],
			    litearray2[user_addr[6:2]],
			    litearray1[user_addr[6:2]],
			    litearray0[user_addr[6:2]] };
     end
   
   // A simple inferred RAM
   /* Original Code
   always @(posedge bus_clk)
     begin
	if (user_w_mem_8_wren)
	  demoarray[user_mem_8_addr] <= user_w_mem_8_data;
	
	if (user_r_mem_8_rden)
	  user_r_mem_8_data <= demoarray[user_mem_8_addr];	  
     end
  */

parameter RESET_ADDR = 0;
parameter SELECT_ADDR = 1;
parameter COUNT_ADDR = 2;

  always @(posedge bus_clk) begin
    if(user_w_mem_8_wren)
      demoarray[user_mem_8_addr] <= user_w_mem_8_data;

    if(user_r_mem_8_rden) begin
      if (user_mem_8_addr == COUNT_ADDR)
        user_r_mem_8_data <= ack_count;
      else
        user_r_mem_8_data <= demoarray[user_mem_8_addr];
    end
  end

   assign  user_r_mem_8_empty = 0;
   assign  user_r_mem_8_eof = 0;
   assign  user_w_mem_8_full = 0;

   /* Original code
   // 32-bit loopback
   fifo_32x512 fifo_32
     (
      .clk(bus_clk),
      .srst(!user_w_write_32_open && !user_r_read_32_open),
      .din(user_w_write_32_data),
      .wr_en(user_w_write_32_wren),
      .rd_en(user_r_read_32_rden),
      .dout(user_r_read_32_data),
      .full(user_w_write_32_full),
      .empty(user_r_read_32_empty)
      );

   assign  user_r_read_32_eof = 0;
   
   // 8-bit loopback
   fifo_8x2048 fifo_8
     (
      .clk(bus_clk),
      .srst(!user_w_write_8_open && !user_r_read_8_open),
      .din(user_w_write_8_data),
      .wr_en(user_w_write_8_wren),
      .rd_en(user_r_read_8_rden),
      .dout(user_r_read_8_data),
      .full(user_w_write_8_full),
      .empty(user_r_read_8_empty)
      );

   assign  user_r_read_8_eof = 0;
   */

  wire sse_reset;
  wire sse_select_ready, sse_select_valid;
  wire sse_img_in_ready, sse_img_in_valid;
  wire sse_img_out_ready, sse_img_out_valid;
  wire [31:0] sse_img_in_bits, sse_img_out_bits;
  wire [7:0] sse_select_bits;

  ScaleSpaceExtrema sse(
    .clk(bus_clk), 
    .reset(sse_reset),
    
    .io_select_ready(sse_select_ready),
    .io_select_valid(sse_select_valid),
    .io_select_bits(sse_select_bits), // 8 bits
    //.io_select_valid(1'b1),
    //.io_select_bits(8'd8), // This static assignment worked to just get a diff out

    .io_img_in_ready(sse_img_in_ready),
    .io_img_in_valid(sse_img_in_valid),
    .io_img_in_bits(sse_img_in_bits[23:0]), // 24 bits
    
    .io_img_out_ready(sse_img_out_ready),
    .io_img_out_valid(sse_img_out_valid),
    .io_img_out_bits(sse_img_out_bits[23:0]) // 24 bits
  );

  // Select FIFO tied to user 8 write port
  wire select_fifo_reset;
  assign select_fifo_reset = !user_w_write_8_open & !user_r_read_8_open;
  wire select_fifo_valid, select_fifo_ready;
  wire [7:0] select_fifo_bits;

  fifo_8x2048_fwft select_fifo(
    .clk(bus_clk),
    .srst(select_fifo_reset),

    .full(user_w_write_8_full),
    .wr_en(user_w_write_8_wren),
    .din(user_w_write_8_data),
    
    //.rd_en(select_fifo_ready),
    .rd_en(1'b1),
    .valid(select_fifo_valid),
    //.dout(sse_select_bits)
    .dout(select_fifo_bits)
  );

  wire ack_fifo_valid;
  assign sse_select_valid = 1'b1;
  assign sse_select_bits = demoarray[SELECT_ADDR];
  assign sse_img_out_bits[31:24] = sse_select_bits;

  //assign ack_fifo_valid = select_fifo_valid & select_fifo_ready;
  assign ack_fifo_valid = sse_select_ready & sse_select_valid;

  reg [7:0] ack_count;

  initial ack_count = 8'd0;

  always @(posedge bus_clk) begin
    if (select_fifo_reset) begin
      ack_count <= 8'd0;
    end
    else if (ack_fifo_valid) begin
      ack_count <= ack_count + 8'd1;
    end
  end

  // Acknowledge when selected stream has been changed
  fifo_8x2048 ack_fifo(
    .clk(bus_clk),
    .srst(select_fifo_reset),

    .full(),
    .wr_en(select_fifo_valid),
    //.din(ack_count),
    .din(select_fifo_bits),
    
    .rd_en(user_r_read_8_rden),
    .empty(user_r_read_8_empty),
    .dout(user_r_read_8_data)
  );

  assign  user_r_read_8_eof = 0;

  // Image input and output FIFOs
  wire img_fifo_reset;
  assign img_fifo_reset = /*sse_reset_condition | */(!user_w_write_32_open & !user_r_read_32_open);
  
  fifo_32x512_fwft img_in_fifo(
    .clk(bus_clk),
    .srst(img_fifo_reset),

    .full(user_w_write_32_full),
    .wr_en(user_w_write_32_wren),
    .din(user_w_write_32_data),
    
    .rd_en(sse_img_in_ready),
    .valid(sse_img_in_valid),
    .dout(sse_img_in_bits)
  );

  wire img_out_fifo_full;
  assign sse_img_out_ready = !img_out_fifo_full;

  fifo_32x512 img_out_fifo(
    .clk(bus_clk),
    .srst(img_fifo_reset),

    .full(img_out_fifo_full),
    .wr_en(sse_img_out_valid),
    .din(sse_img_out_bits),
    
    .rd_en(user_r_read_32_rden),
    .empty(user_r_read_32_empty),
    .dout(user_r_read_32_data)
  );

  assign  user_r_read_32_eof = 0;

  /*reg sse_has_been_reset;
  initial sse_has_been_reset = 1'b1;

  always @(posedge bus_clk) begin
    if(sse_reset)
      sse_has_been_reset <= 1'b1;
    else if(sse_img_in_valid)
      sse_has_been_reset <= 1'b0;
  end

  wire sse_reset_condition;
  assign sse_reset_condition = (sse_select_ready & select_fifo_valid &
    (select_fifo_bits == 8'hFF));

  assign sse_select_valid = select_fifo_valid & sse_has_been_reset;
  assign select_fifo_ready = sse_select_ready & sse_has_been_reset;*/

  assign sse_reset = (demoarray[RESET_ADDR] == 8'hFF);
  //(select_fifo_reset | img_fifo_reset | 
  //  (demoarray[RESET_ADDR] == 8'hFF));
  //  (sse_reset_condition & !sse_has_been_reset));
  
  // Extra status LEDs
  //assign PS_GPIO[7] = select_fifo_reset; // LD4
  //assign PS_GPIO[8] = img_fifo_reset; // LD5
  //assign PS_GPIO[9] = sse_reset; // LD6
  //assign PS_GPIO[10] = sse_reset_condition; // LD7

   i2s_audio audio
     (
      .bus_clk(bus_clk),
      .clk_100(clk_100),
      .quiesce(quiesce),

      .audio_mclk(audio_mclk),
      .audio_dac(audio_dac),
      .audio_adc(audio_adc),
      .audio_bclk(audio_bclk),
      .audio_lrclk(audio_lrclk),
      
      .user_r_audio_rden(user_r_audio_rden),
      .user_r_audio_empty(user_r_audio_empty),
      .user_r_audio_data(user_r_audio_data),
      .user_r_audio_eof(user_r_audio_eof),
      .user_r_audio_open(user_r_audio_open),
      
      .user_w_audio_wren(user_w_audio_wren),
      .user_w_audio_full(user_w_audio_full),
      .user_w_audio_data(user_w_audio_data),
      .user_w_audio_open(user_w_audio_open)
      );
   
   smbus smbus
     (
      .bus_clk(bus_clk),
      .quiesce(quiesce),

      .smb_sclk(smb_sclk),
      .smb_sdata(smb_sdata),
      .smbus_addr(smbus_addr),

      .user_r_smb_rden(user_r_smb_rden),
      .user_r_smb_empty(user_r_smb_empty),
      .user_r_smb_data(user_r_smb_data),
      .user_r_smb_eof(user_r_smb_eof),
      .user_r_smb_open(user_r_smb_open),
      
      .user_w_smb_wren(user_w_smb_wren),
      .user_w_smb_full(user_w_smb_full),
      .user_w_smb_data(user_w_smb_data),
      .user_w_smb_open(user_w_smb_open)
      );

endmodule
